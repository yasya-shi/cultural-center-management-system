package com.culturalcenter.server.controllers;

import com.culturalcenter.server.utils.DatabaseConnection;
import com.culturalcenter.common.PasswordUtil;
import com.culturalcenter.server.network.Response;
import com.culturalcenter.server.serializer.Serializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class AuthController {

    // Вход в систему
    public Response login(String data) {
        try {
            Map<String, String> credentials = Serializer.fromJson(data, Map.class);
            String login = credentials.get("login");
            String password = credentials.get("password");

            if (login == null || password == null) {
                return new Response(false, "Не указаны логин или пароль", null);
            }

            String sql = "SELECT user_id, password_hash, role, full_name, status FROM users WHERE login = ?";

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, login);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String status = rs.getString("status");
                    if (!"ACTIVE".equals(status)) {
                        return new Response(false, "Учётная запись заблокирована", null);
                    }

                    String hashedPassword = rs.getString("password_hash");

                    // ✅ ИСПРАВЛЕНО
                    if (PasswordUtil.getInstance().checkPassword(password, hashedPassword)) {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("userId", rs.getInt("user_id"));
                        userData.put("login", login);
                        userData.put("role", rs.getString("role"));
                        userData.put("fullName", rs.getString("full_name"));

                        return new Response(true, "Вход выполнен успешно", Serializer.toJson(userData));
                    } else {
                        return new Response(false, "Неверный пароль", null);
                    }
                } else {
                    return new Response(false, "Пользователь не найден", null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка сервера: " + e.getMessage(), null);
        }
    }

    // Регистрация нового организатора
    public Response register(String data) {
        try {
            Map<String, String> userData = Serializer.fromJson(data, Map.class);

            String login = userData.get("login");
            String password = userData.get("password");
            String fullName = userData.get("fullName");
            String email = userData.get("email");
            String phone = userData.get("phone");

            if (login == null || login.length() < 4) {
                return new Response(false, "Логин должен содержать не менее 4 символов", null);
            }
            if (password == null || password.length() < 6) {
                return new Response(false, "Пароль должен содержать не менее 6 символов", null);
            }
            if (fullName == null || fullName.isEmpty()) {
                return new Response(false, "Введите ФИО", null);
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

            String insertSql = "INSERT INTO users (login, password_hash, full_name, email, phone, role, status) VALUES (?, ?, ?, ?, ?, 'ORGANIZER', 'ACTIVE')";

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, login);
                // ✅ УЖЕ ПРАВИЛЬНО
                insertStmt.setString(2, PasswordUtil.getInstance().hashPassword(password));
                insertStmt.setString(3, fullName);
                insertStmt.setString(4, email);
                insertStmt.setString(5, phone);
                insertStmt.executeUpdate();

                return new Response(true, "Регистрация прошла успешно", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка сервера: " + e.getMessage(), null);
        }
    }

    // Получение данных пользователя по логину
    public Response getUser(String data) {
        try {
            String login = Serializer.fromJson(data, String.class);

            String sql = "SELECT user_id, login, full_name, email, phone, role FROM users WHERE login = ?";

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, login);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("userId", rs.getInt("user_id"));
                    userData.put("login", rs.getString("login"));
                    userData.put("fullName", rs.getString("full_name"));
                    userData.put("email", rs.getString("email"));
                    userData.put("phone", rs.getString("phone"));
                    userData.put("role", rs.getString("role"));

                    return new Response(true, "Данные получены", Serializer.toJson(userData));
                } else {
                    return new Response(false, "Пользователь не найден", null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка сервера: " + e.getMessage(), null);
        }
    }

    // Обновление данных пользователя
    public Response updateUser(String data) {
        try {
            Map<String, String> userData = Serializer.fromJson(data, Map.class);

            String login = userData.get("login");
            String fullName = userData.get("fullName");
            String email = userData.get("email");
            String phone = userData.get("phone");
            String newPassword = userData.get("newPassword");

            if (fullName == null || fullName.isEmpty()) {
                return new Response(false, "ФИО не может быть пустым", null);
            }
            if (email == null || !email.contains("@")) {
                return new Response(false, "Введите корректный Email", null);
            }

            String sql;
            if (newPassword != null && !newPassword.isEmpty()) {
                sql = "UPDATE users SET full_name = ?, email = ?, phone = ?, password_hash = ? WHERE login = ?";
            } else {
                sql = "UPDATE users SET full_name = ?, email = ?, phone = ? WHERE login = ?";
            }

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                if (newPassword != null && !newPassword.isEmpty()) {
                    pstmt.setString(1, fullName);
                    pstmt.setString(2, email);
                    pstmt.setString(3, phone == null || phone.isEmpty() ? null : phone);
                    // ✅ ИСПРАВЛЕНО
                    pstmt.setString(4, PasswordUtil.getInstance().hashPassword(newPassword));
                    pstmt.setString(5, login);
                } else {
                    pstmt.setString(1, fullName);
                    pstmt.setString(2, email);
                    pstmt.setString(3, phone == null || phone.isEmpty() ? null : phone);
                    pstmt.setString(4, login);
                }

                int updated = pstmt.executeUpdate();
                if (updated > 0) {
                    return new Response(true, "Профиль успешно обновлён", null);
                } else {
                    return new Response(false, "Пользователь не найден", null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка сервера: " + e.getMessage(), null);
        }
    }

    // Получение пользователя по ID (для админа)
    public Response getUserById(String data) {
        try {
            int userId = Integer.parseInt(Serializer.fromJson(data, String.class));
            String sql = "SELECT user_id, login, full_name, email, phone, role, status FROM users WHERE user_id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("userId", rs.getInt("user_id"));
                    userData.put("login", rs.getString("login"));
                    userData.put("fullName", rs.getString("full_name"));
                    userData.put("email", rs.getString("email"));
                    userData.put("phone", rs.getString("phone"));
                    userData.put("role", rs.getString("role"));
                    userData.put("status", rs.getString("status"));
                    return new Response(true, "Данные получены", Serializer.toJson(userData));
                } else {
                    return new Response(false, "Пользователь не найден", null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Ошибка сервера: " + e.getMessage(), null);
        }
    }
}