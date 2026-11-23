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

public class CulturalCenterController {

    public Response getAllCenters() {
        try {
            String sql = "SELECT c.center_id, c.name, c.address, c.phone, c.email, u.full_name as director_name " +
                    "FROM cultural_centers c LEFT JOIN users u ON c.director_id = u.user_id ORDER BY c.center_id";
            List<Map<String, Object>> centers = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> center = new HashMap<>();
                    center.put("centerId", rs.getInt("center_id"));
                    center.put("name", rs.getString("name"));
                    center.put("address", rs.getString("address"));
                    center.put("phone", rs.getString("phone"));
                    center.put("email", rs.getString("email"));
                    center.put("directorName", rs.getString("director_name") == null ? "Не назначен" : rs.getString("director_name"));
                    centers.add(center);
                }
            }
            return new Response(true, "Список центров получен", Serializer.toJson(centers));
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response getCenterById(String data) {
        try {
            int centerId = Integer.parseInt(Serializer.fromJson(data, String.class));
            String sql = "SELECT center_id, name, address, phone, email, director_id FROM cultural_centers WHERE center_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, centerId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    Map<String, Object> center = new HashMap<>();
                    center.put("centerId", rs.getInt("center_id"));
                    center.put("name", rs.getString("name"));
                    center.put("address", rs.getString("address"));
                    center.put("phone", rs.getString("phone"));
                    center.put("email", rs.getString("email"));
                    center.put("directorId", rs.getObject("director_id"));
                    return new Response(true, "Центр найден", Serializer.toJson(center));
                }
                return new Response(false, "Центр не найден", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response createCenter(String data) {
        try {
            Map<String, String> centerData = Serializer.fromJson(data, Map.class);
            String name = centerData.get("name");
            String address = centerData.get("address");
            String phone = centerData.get("phone");
            String email = centerData.get("email");
            String sql = "INSERT INTO cultural_centers (name, address, phone, email) VALUES (?, ?, ?, ?)";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, name);
                pstmt.setString(2, address);
                pstmt.setString(3, phone);
                pstmt.setString(4, email);
                pstmt.executeUpdate();
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                int centerId = -1;
                if (generatedKeys.next()) {
                    centerId = generatedKeys.getInt(1);
                }
                Map<String, Integer> result = new HashMap<>();
                result.put("centerId", centerId);
                return new Response(true, "Центр создан", Serializer.toJson(result));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response updateCenter(String data) {
        try {
            Map<String, Object> centerData = Serializer.fromJson(data, Map.class);
            int centerId = ((Number) centerData.get("centerId")).intValue();
            String name = (String) centerData.get("name");
            String address = (String) centerData.get("address");
            String phone = (String) centerData.get("phone");
            String email = (String) centerData.get("email");
            String sql = "UPDATE cultural_centers SET name = ?, address = ?, phone = ?, email = ? WHERE center_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.setString(2, address);
                pstmt.setString(3, phone);
                pstmt.setString(4, email);
                pstmt.setInt(5, centerId);
                pstmt.executeUpdate();
            }
            return new Response(true, "Центр обновлён", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response deleteCenter(String data) {
        try {
            int centerId = Integer.parseInt(Serializer.fromJson(data, String.class));
            String checkSql = "SELECT COUNT(*) FROM bookings b JOIN rooms r ON b.room_id = r.room_id WHERE r.center_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, centerId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return new Response(false, "Нельзя удалить центр: есть заявки на помещения", null);
                }
            }
            String deleteSql = "DELETE FROM cultural_centers WHERE center_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, centerId);
                pstmt.executeUpdate();
            }
            return new Response(true, "Центр удалён", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response getCentersForDropdown() {
        try {
            String sql = "SELECT center_id, name FROM cultural_centers ORDER BY name";
            List<Map<String, Object>> centers = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> center = new HashMap<>();
                    center.put("centerId", rs.getInt("center_id"));
                    center.put("name", rs.getString("name"));
                    centers.add(center);
                }
            }
            return new Response(true, "Список центров получен", Serializer.toJson(centers));
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response getMyCenter(String data) {
        try {
            // data — это просто строка с логином
            String login = data;

            System.out.println("getMyCenter вызван с логином: [" + login + "]");

            // Исправленный SQL
            String sql = "SELECT c.center_id, c.name, c.address, c.phone, c.email " +
                    "FROM cultural_centers c " +
                    "WHERE c.director_id = (SELECT user_id FROM users WHERE login = ?)";

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, login);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    Map<String, Object> center = new HashMap<>();
                    center.put("centerId", rs.getInt("center_id"));
                    center.put("name", rs.getString("name"));
                    center.put("address", rs.getString("address"));
                    center.put("phone", rs.getString("phone"));
                    center.put("email", rs.getString("email"));

                    System.out.println("Центр найден: " + rs.getString("name"));
                    return new Response(true, "Центр найден", Serializer.toJson(center));
                } else {
                    System.out.println("Центр не найден для логина: " + login);
                    return new Response(false, "Центр не найден", null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }
}