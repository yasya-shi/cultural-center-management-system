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
import java.time.LocalDateTime;

public class BookingController {

    public Response getBookingsByCenter(String data) {
        try {
            int centerId = Integer.parseInt(Serializer.fromJson(data, String.class));

            String sql = "SELECT b.booking_id, b.event_date, b.start_time, b.end_time, b.purpose, b.status, " +
                    "r.name as room_name, u.full_name as organizer_name, b.rejection_reason " +
                    "FROM bookings b " +
                    "JOIN rooms r ON b.room_id = r.room_id " +
                    "JOIN users u ON b.organizer_id = u.user_id " +
                    "WHERE r.center_id = ? " +
                    "ORDER BY FIELD(b.status, 'PENDING', 'APPROVED', 'REJECTED'), b.event_date, b.start_time";

            List<Map<String, Object>> bookings = new ArrayList<>();

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, centerId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Map<String, Object> booking = new HashMap<>();
                    booking.put("bookingId", rs.getInt("booking_id"));
                    booking.put("roomName", rs.getString("room_name"));
                    booking.put("organizerName", rs.getString("organizer_name"));
                    booking.put("eventDate", rs.getString("event_date"));
                    booking.put("startTime", rs.getString("start_time"));
                    booking.put("endTime", rs.getString("end_time"));
                    booking.put("purpose", rs.getString("purpose"));
                    booking.put("status", rs.getString("status"));
                    booking.put("rejectionReason", rs.getString("rejection_reason") != null ? rs.getString("rejection_reason") : "");
                    bookings.add(booking);
                }
            }

            return new Response(true, "Заявки получены", Serializer.toJson(bookings));
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response approveBooking(String data) {
        try {
            Map<String, Object> requestData = Serializer.fromJson(data, Map.class);
            int bookingId = ((Number) requestData.get("bookingId")).intValue();

            // Получаем room_id, дату, время из заявки
            String getSql = "SELECT room_id, event_date, start_time, end_time FROM bookings WHERE booking_id = ?";
            int roomId = -1;
            String eventDate = null;
            String startTime = null;
            String endTime = null;

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(getSql)) {
                pstmt.setInt(1, bookingId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    roomId = rs.getInt("room_id");
                    eventDate = rs.getString("event_date");
                    startTime = rs.getString("start_time");
                    endTime = rs.getString("end_time");
                } else {
                    return new Response(false, "Заявка не найдена", null);
                }
            }

            // Проверка конфликта бронирований
            String checkSql = "SELECT COUNT(*) FROM bookings WHERE room_id = ? AND event_date = ? " +
                    "AND status = 'APPROVED' AND ((start_time <= ? AND end_time > ?) OR (start_time < ? AND end_time >= ?))";

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setInt(1, roomId);
                pstmt.setString(2, eventDate);
                pstmt.setString(3, startTime);
                pstmt.setString(4, startTime);
                pstmt.setString(5, endTime);
                pstmt.setString(6, endTime);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return new Response(false, "Конфликт: зал уже занят в это время", null);
                }
            }

            // Обновляем статус заявки
            String updateSql = "UPDATE bookings SET status = 'APPROVED' WHERE booking_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setInt(1, bookingId);
                pstmt.executeUpdate();
            }

            // Уведомление организатору
            createNotification(bookingId, "Ваша заявка подтверждена");

            return new Response(true, "Заявка подтверждена", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response rejectBooking(String data) {
        try {
            Map<String, Object> requestData = Serializer.fromJson(data, Map.class);
            int bookingId = ((Number) requestData.get("bookingId")).intValue();
            String reason = (String) requestData.get("reason");

            if (reason == null || reason.isEmpty()) {
                return new Response(false, "Укажите причину отказа", null);
            }

            String sql = "UPDATE bookings SET status = 'REJECTED', rejection_reason = ? WHERE booking_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, reason);
                pstmt.setInt(2, bookingId);
                pstmt.executeUpdate();
            }

            // Уведомление организатору с причиной
            createNotification(bookingId, "Заявка отклонена. Причина: " + reason);

            return new Response(true, "Заявка отклонена", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }
//Паттерн OBSERVER (Наблюдатель)
    private void createNotification(int bookingId, String message) {
        try {
            String getSql = "SELECT organizer_id FROM bookings WHERE booking_id = ?";
            int organizerId = -1;
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(getSql)) {
                pstmt.setInt(1, bookingId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    organizerId = rs.getInt("organizer_id");
                }
            }

            if (organizerId != -1) {
                String insertSql = "INSERT INTO notifications (user_id, message, is_read, created_at) VALUES (?, ?, 0, NOW())";
                try (Connection conn = DatabaseConnection.getInstance().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setInt(1, organizerId);
                    pstmt.setString(2, message);
                    pstmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Создание заявки организатором
    public Response createBooking(String data) {
        try {
            Map<String, Object> bookingData = Serializer.fromJson(data, Map.class);

            int roomId = ((Number) bookingData.get("roomId")).intValue();
            int organizerId = ((Number) bookingData.get("organizerId")).intValue();
            String eventDate = (String) bookingData.get("eventDate");
            String startTime = (String) bookingData.get("startTime");
            String endTime = (String) bookingData.get("endTime");
            String purpose = (String) bookingData.get("purpose");

            // Проверка конфликта (есть ли уже APPROVED заявки на это время)
            String checkSql = "SELECT COUNT(*) FROM bookings WHERE room_id = ? AND event_date = ? " +
                    "AND status = 'APPROVED' AND ((start_time <= ? AND end_time > ?) OR (start_time < ? AND end_time >= ?))";

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setInt(1, roomId);
                pstmt.setString(2, eventDate);
                pstmt.setString(3, startTime);
                pstmt.setString(4, startTime);
                pstmt.setString(5, endTime);
                pstmt.setString(6, endTime);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return new Response(false, "Это время уже занято", null);
                }
            }

            // Проверка, открыт ли зал (status = OPEN)
            String checkRoomSql = "SELECT status FROM rooms WHERE room_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(checkRoomSql)) {
                pstmt.setInt(1, roomId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && !"OPEN".equals(rs.getString("status"))) {
                    return new Response(false, "Зал временно закрыт", null);
                }
            }

            // Создание заявки
            String sql = "INSERT INTO bookings (room_id, organizer_id, event_date, start_time, end_time, purpose, status, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'PENDING', NOW())";

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, roomId);
                pstmt.setInt(2, organizerId);
                pstmt.setString(3, eventDate);
                pstmt.setString(4, startTime);
                pstmt.setString(5, endTime);
                pstmt.setString(6, purpose);
                pstmt.executeUpdate();
            }

            // Уведомление директору
            notifyDirector(roomId);

            return new Response(true, "Заявка отправлена на рассмотрение", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    // Получить заявки организатора
    public Response getMyBookings(String data) {
        try {
            int organizerId = Integer.parseInt(data);

            String sql = "SELECT b.booking_id, b.event_date, b.start_time, b.end_time, b.purpose, b.status, b.rejection_reason, " +
                    "r.name as room_name, c.name as center_name " +
                    "FROM bookings b " +
                    "JOIN rooms r ON b.room_id = r.room_id " +
                    "JOIN cultural_centers c ON r.center_id = c.center_id " +
                    "WHERE b.organizer_id = ? " +
                    "ORDER BY b.event_date DESC, b.start_time DESC";

            List<Map<String, Object>> bookings = new ArrayList<>();

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, organizerId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Map<String, Object> booking = new HashMap<>();
                    booking.put("bookingId", rs.getInt("booking_id"));
                    booking.put("roomName", rs.getString("room_name"));
                    booking.put("centerName", rs.getString("center_name"));
                    booking.put("eventDate", rs.getString("event_date"));
                    booking.put("startTime", rs.getString("start_time"));
                    booking.put("endTime", rs.getString("end_time"));
                    booking.put("purpose", rs.getString("purpose"));
                    booking.put("status", rs.getString("status"));
                    booking.put("rejectionReason", rs.getString("rejection_reason") != null ? rs.getString("rejection_reason") : "");
                    bookings.add(booking);
                }
            }

            return new Response(true, "Заявки получены", Serializer.toJson(bookings));
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    // Отмена заявки организатором
    public Response cancelBooking(String data) {
        try {
            int bookingId = Integer.parseInt(data);

            // Проверка: заявка должна быть в статусе PENDING
            // и до мероприятия больше 24 часов
            String checkSql = "SELECT status, event_date, start_time FROM bookings WHERE booking_id = ?";
            String status = null;
            String eventDate = null;
            String startTime = null;

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setInt(1, bookingId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    status = rs.getString("status");
                    eventDate = rs.getString("event_date");
                    startTime = rs.getString("start_time");
                } else {
                    return new Response(false, "Заявка не найдена", null);
                }
            }

            if (!"PENDING".equals(status)) {
                return new Response(false, "Можно отменить только заявки в статусе 'На рассмотрении'", null);
            }

            // Проверка: до мероприятия больше 24 часов?
            LocalDateTime eventDateTime = LocalDateTime.parse(eventDate + "T" + startTime);
            LocalDateTime now = LocalDateTime.now();
            if (now.plusHours(24).isAfter(eventDateTime)) {
                return new Response(false, "Отмена невозможна: до мероприятия менее 24 часов", null);
            }

            String sql = "UPDATE bookings SET status = 'CANCELLED' WHERE booking_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, bookingId);
                pstmt.executeUpdate();
            }

            return new Response(true, "Заявка отменена", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    // Вспомогательный метод — уведомить директора
    private void notifyDirector(int roomId) {
        try {
            String sql = "SELECT c.director_id FROM cultural_centers c JOIN rooms r ON c.center_id = r.center_id WHERE r.room_id = ?";
            int directorId = -1;
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, roomId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    directorId = rs.getInt("director_id");
                }
            }

            if (directorId != -1) {
                String insertSql = "INSERT INTO notifications (user_id, message, is_read, created_at) VALUES (?, ?, 0, NOW())";
                try (Connection conn = DatabaseConnection.getInstance().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setInt(1, directorId);
                    pstmt.setString(2, "Поступила новая заявка на бронирование");
                    pstmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Response getUnreadNotifications(String data) {
        try {
            int userId = Integer.parseInt(data);

            // Берём ТОЛЬКО ОДНО последнее уведомление
            String sql = "SELECT notification_id, message, created_at FROM notifications WHERE user_id = ? AND is_read = 0 ORDER BY created_at DESC LIMIT 1";

            List<Map<String, Object>> notifications = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    Map<String, Object> n = new HashMap<>();
                    n.put("id", rs.getInt("notification_id"));
                    n.put("message", rs.getString("message"));
                    n.put("createdAt", rs.getString("created_at"));
                    notifications.add(n);
                }
            }

            // Помечаем ЭТО одно как прочитанное
            if (!notifications.isEmpty()) {
                String updateSql = "UPDATE notifications SET is_read = 1 WHERE notification_id = ?";
                try (Connection conn = DatabaseConnection.getInstance().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setInt(1, (Integer) notifications.get(0).get("id"));
                    pstmt.executeUpdate();
                }
            }

            return new Response(true, "Уведомления получены", Serializer.toJson(notifications));
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response updateCompletedBookings() {
        try {
            String sql = "UPDATE bookings SET status = 'COMPLETED' WHERE status = 'APPROVED' AND event_date < CURDATE()";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int updated = pstmt.executeUpdate();
                System.out.println("Обновлено завершённых заявок: " + updated);
            }
            return new Response(true, "Статусы обновлены", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }
}