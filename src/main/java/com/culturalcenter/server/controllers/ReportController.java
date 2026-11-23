package com.culturalcenter.server.controllers;

import com.culturalcenter.server.network.Response;
import com.culturalcenter.server.serializer.Serializer;
import com.culturalcenter.server.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportController {

    public Response generateReport(String data) {
        try {
            Map<String, String> params = Serializer.fromJson(data, Map.class);
            int centerId = Integer.parseInt(params.get("centerId"));
            String startDate = params.get("startDate");
            String endDate = params.get("endDate");
            String reportType = params.get("reportType");

            List<Map<String, Object>> result = new ArrayList<>();

            String sql = "SELECT r.name as room_name, " +
                    "COALESCE(SUM(TIMESTAMPDIFF(HOUR, b.start_time, b.end_time)), 0) as hours, " +
                    "COUNT(b.booking_id) as booking_count, " +
                    "COALESCE(AVG(rv.rating), 0) as avg_rating " +
                    "FROM rooms r " +
                    "LEFT JOIN bookings b ON r.room_id = b.room_id AND b.status IN ('APPROVED', 'COMPLETED') " +
                    "AND b.event_date BETWEEN ? AND ? " +
                    "LEFT JOIN reviews rv ON b.booking_id = rv.booking_id " +
                    "WHERE r.center_id = ? " +
                    "GROUP BY r.room_id, r.name";

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, startDate);
                pstmt.setString(2, endDate);
                pstmt.setInt(3, centerId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("roomName", rs.getString("room_name"));
                    row.put("hours", rs.getDouble("hours"));
                    row.put("bookingCount", rs.getInt("booking_count"));
                    row.put("averageRating", rs.getDouble("avg_rating"));
                    result.add(row);
                }
            }

            return new Response(true, "Отчёт сформирован", Serializer.toJson(result));
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }
}