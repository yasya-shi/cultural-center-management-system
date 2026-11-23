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

public class RoomController {

    public Response getRoomsByCenterId(String data) {
        try {
            int centerId = Integer.parseInt(data);

            String sql = "SELECT room_id, name, capacity, equipment, price_per_hour, IFNULL(status, 'OPEN') as status " +
                    "FROM rooms WHERE center_id = ? ORDER BY room_id";

            List<Map<String, Object>> rooms = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, centerId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Map<String, Object> room = new HashMap<>();
                    room.put("roomId", rs.getInt("room_id"));
                    room.put("name", rs.getString("name"));
                    room.put("capacity", rs.getInt("capacity"));
                    room.put("equipment", rs.getString("equipment"));
                    room.put("pricePerHour", rs.getDouble("price_per_hour"));
                    room.put("status", rs.getString("status"));
                    rooms.add(room);
                }
            }

            return new Response(true, "Список залов получен", Serializer.toJson(rooms));
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response createRoom(String data) {
        try {
            Map<String, Object> roomData = Serializer.fromJson(data, Map.class);
            int centerId = ((Number) roomData.get("centerId")).intValue();
            String name = (String) roomData.get("name");
            int capacity = ((Number) roomData.get("capacity")).intValue();
            String equipment = (String) roomData.get("equipment");
            double pricePerHour = ((Number) roomData.get("pricePerHour")).doubleValue();
            String sql = "INSERT INTO rooms (center_id, name, capacity, equipment, price_per_hour) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, centerId);
                pstmt.setString(2, name);
                pstmt.setInt(3, capacity);
                pstmt.setString(4, equipment);
                pstmt.setDouble(5, pricePerHour);
                pstmt.executeUpdate();
            }
            return new Response(true, "Зал добавлен", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response updateRoom(String data) {
        try {
            Map<String, Object> roomData = Serializer.fromJson(data, Map.class);
            int roomId = ((Number) roomData.get("roomId")).intValue();
            String name = (String) roomData.get("name");
            int capacity = ((Number) roomData.get("capacity")).intValue();
            String equipment = (String) roomData.get("equipment");
            double pricePerHour = ((Number) roomData.get("pricePerHour")).doubleValue();
            String sql = "UPDATE rooms SET name = ?, capacity = ?, equipment = ?, price_per_hour = ? WHERE room_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.setInt(2, capacity);
                pstmt.setString(3, equipment);
                pstmt.setDouble(4, pricePerHour);
                pstmt.setInt(5, roomId);
                pstmt.executeUpdate();
            }
            return new Response(true, "Зал обновлён", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response deleteRoom(String data) {
        try {
            int roomId = Integer.parseInt(Serializer.fromJson(data, String.class));
            String checkSql = "SELECT COUNT(*) FROM bookings WHERE room_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, roomId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return new Response(false, "Нельзя удалить зал: есть заявки на это помещение", null);
                }
            }
            String deleteSql = "DELETE FROM rooms WHERE room_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, roomId);
                pstmt.executeUpdate();
            }
            return new Response(true, "Зал удалён", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response checkRoomHasBookings(String data) {
        try {
            int roomId = Integer.parseInt(Serializer.fromJson(data, String.class));
            String sql = "SELECT COUNT(*) FROM bookings WHERE room_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, roomId);
                ResultSet rs = pstmt.executeQuery();
                boolean hasBookings = rs.next() && rs.getInt(1) > 0;
                return new Response(true, "OK", Serializer.toJson(hasBookings));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }
    // Добавить в конец класса перед последней скобкой

    public Response closeRoom(String data) {
        try {
            int roomId = Integer.parseInt(Serializer.fromJson(data, String.class));

            // Проверка на активные заявки
            String checkSql = "SELECT COUNT(*) FROM bookings WHERE room_id = ? AND status IN ('PENDING', 'APPROVED')";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setInt(1, roomId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return new Response(false, "Нельзя закрыть зал: есть активные заявки", null);
                }
            }

            String sql = "UPDATE rooms SET status = 'CLOSED' WHERE room_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, roomId);
                pstmt.executeUpdate();
            }
            return new Response(true, "Зал закрыт", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response openRoom(String data) {
        try {
            int roomId = Integer.parseInt(Serializer.fromJson(data, String.class));

            String sql = "UPDATE rooms SET status = 'OPEN' WHERE room_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, roomId);
                pstmt.executeUpdate();
            }
            return new Response(true, "Зал открыт", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }
}