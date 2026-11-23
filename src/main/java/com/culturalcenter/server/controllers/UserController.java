package com.culturalcenter.server.controllers;

import com.culturalcenter.common.PasswordUtil;
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

public class UserController {

    public Response getAllUsers() {
        try {
            String sql = "SELECT user_id, login, full_name, email, phone, role, status FROM users ORDER BY user_id";
            List<Map<String, Object>> users = new ArrayList<>();

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("userId", rs.getInt("user_id"));
                    user.put("login", rs.getString("login"));
                    user.put("fullName", rs.getString("full_name"));
                    user.put("email", rs.getString("email"));
                    user.put("phone", rs.getString("phone"));
                    user.put("role", rs.getString("role"));
                    user.put("status", rs.getString("status"));
                    users.add(user);
                }
            }
            return new Response(true, "Список пользователей получен", Serializer.toJson(users));
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response createUser(String data) {
        try {
            Map<String, String> userData = Serializer.fromJson(data, Map.class);
            String login = userData.get("login");
            String password = userData.get("password");
            String fullName = userData.get("fullName");
            String email = userData.get("email");
            String phone = userData.get("phone");
            String role = userData.get("role");

            if (login == null || login.length() < 4) {
                return new Response(false, "Логин должен быть не менее 4 символов", null);
            }
            if (password == null || password.length() < 6) {
                return new Response(false, "Пароль должен быть не менее 6 символов", null);
            }
            if (fullName == null || fullName.isEmpty()) {
                return new Response(false, "ФИО обязательно", null);
            }

            String checkSql = "SELECT 1 FROM users WHERE login = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, login);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    return new Response(false, "Пользователь с таким логином уже существует", null);
                }
            }

            String insertSql = "INSERT INTO users (login, password_hash, full_name, email, phone, role, status) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, login);
                pstmt.setString(2, PasswordUtil.getInstance().hashPassword(password));
                pstmt.setString(3, fullName);
                pstmt.setString(4, email);
                pstmt.setString(5, phone == null || phone.isEmpty() ? null : phone);
                pstmt.setString(6, role);
                pstmt.executeUpdate();
            }
            return new Response(true, "Пользователь успешно создан", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response updateUserByAdmin(String data) {
        try {
            Map<String, String> userData = Serializer.fromJson(data, Map.class);
            int userId = Integer.parseInt(userData.get("userId"));
            String fullName = userData.get("fullName");
            String email = userData.get("email");
            String phone = userData.get("phone");
            String role = userData.get("role");

            String sql = "UPDATE users SET full_name = ?, email = ?, phone = ?, role = ? WHERE user_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fullName);
                pstmt.setString(2, email);
                pstmt.setString(3, phone == null || phone.isEmpty() ? null : phone);
                pstmt.setString(4, role);
                pstmt.setInt(5, userId);
                pstmt.executeUpdate();
            }
            return new Response(true, "Данные пользователя обновлены", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response deleteUser(String data) {
        try {
            int userId = Integer.parseInt(Serializer.fromJson(data, String.class));
            String checkSql = "SELECT COUNT(*) FROM bookings WHERE organizer_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, userId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return new Response(false, "Нельзя удалить: у пользователя есть заявки", null);
                }
            }
            String deleteSql = "DELETE FROM users WHERE user_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, userId);
                pstmt.executeUpdate();
            }
            return new Response(true, "Пользователь удалён", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response blockUser(String data) {
        try {
            int userId = Integer.parseInt(Serializer.fromJson(data, String.class));
            String sql = "UPDATE users SET status = 'BLOCKED' WHERE user_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.executeUpdate();
            }
            return new Response(true, "Пользователь заблокирован", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response unblockUser(String data) {
        try {
            int userId = Integer.parseInt(Serializer.fromJson(data, String.class));
            String sql = "UPDATE users SET status = 'ACTIVE' WHERE user_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.executeUpdate();
            }
            return new Response(true, "Пользователь разблокирован", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }

    public Response assignDirector(String data) {
        try {
            Map<String, Object> assignData = Serializer.fromJson(data, Map.class);

            int userId = ((Number) assignData.get("userId")).intValue();
            int centerId = ((Number) assignData.get("centerId")).intValue();

            // ✅ ПРОВЕРКА: есть ли уже директор у этого центра
            String checkSql = "SELECT director_id FROM cultural_centers WHERE center_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, centerId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    int currentDirectorId = rs.getInt("director_id");
                    if (currentDirectorId > 0 && currentDirectorId != userId) {
                        return new Response(false, "У этого культурного центра уже есть директор. Сначала удалите текущего.", null);
                    }
                }
            }

            // Назначаем директора
            String sql = "UPDATE cultural_centers SET director_id = ? WHERE center_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, centerId);
                pstmt.executeUpdate();
            }

            return new Response(true, "Директор назначен", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }
}