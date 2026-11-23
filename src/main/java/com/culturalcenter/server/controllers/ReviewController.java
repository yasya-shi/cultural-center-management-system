package com.culturalcenter.server.controllers;

import com.culturalcenter.server.network.Response;
import com.culturalcenter.server.serializer.Serializer;
import com.culturalcenter.server.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.time.LocalDate;

public class ReviewController {

    // Оставить отзыв
    public Response createReview(String data) {
        try {
            Map<String, Object> reviewData = Serializer.fromJson(data, Map.class);

            int bookingId = ((Number) reviewData.get("bookingId")).intValue();
            int rating = ((Number) reviewData.get("rating")).intValue();
            String comment = (String) reviewData.get("comment");

            if (rating < 1 || rating > 5) {
                return new Response(false, "Оценка должна быть от 1 до 5", null);
            }

            // Проверка: можно ли оставить отзыв (заявка подтверждена и дата прошла)
            String checkSql = "SELECT b.status, b.event_date " +
                    "FROM bookings b WHERE b.booking_id = ?";

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setInt(1, bookingId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String status = rs.getString("status");
                    String eventDate = rs.getString("event_date");

                    if (!"COMPLETED".equals(status)) {
                        return new Response(false, "Отзыв можно оставить только к завершённому мероприятию", null);
                    }

                    if (LocalDate.parse(eventDate).isAfter(LocalDate.now())) {
                        return new Response(false, "Отзыв можно оставить только после даты мероприятия", null);
                    }
                } else {
                    return new Response(false, "Заявка не найдена", null);
                }
            }

            // Проверка: не оставлял ли уже отзыв
            String checkReviewSql = "SELECT COUNT(*) FROM reviews WHERE booking_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(checkReviewSql)) {
                pstmt.setInt(1, bookingId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return new Response(false, "Вы уже оставили отзыв на это мероприятие", null);
                }
            }

            String sql = "INSERT INTO reviews (booking_id, rating, comment, created_at) VALUES (?, ?, ?, NOW())";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, bookingId);
                pstmt.setInt(2, rating);
                pstmt.setString(3, comment);
                pstmt.executeUpdate();
            }

            return new Response(true, "Спасибо за отзыв!", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }
}