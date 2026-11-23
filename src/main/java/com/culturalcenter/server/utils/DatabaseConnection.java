package com.culturalcenter.server.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConnection {

    // единственный экземпляр
    private static DatabaseConnection instance;

    private String url;
    private String user;
    private String password;

    // приватный конструктор
    private DatabaseConnection() {
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties props = new Properties();
            props.load(input);
            this.url = props.getProperty("db.url");
            this.user = props.getProperty("db.user");
            this.password = props.getProperty("db.password");
        } catch (Exception e) {
            // значения по умолчанию
            this.url = "jdbc:mysql://localhost:3306/cultural_center_db?useSSL=false&serverTimezone=UTC";
            this.user = "root";
            this.password = "12345678";
        }
    }

    // глобальная точка доступа
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("Подключение к БД cultural_center_db успешно!");
            return conn;
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver не найден!");
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            System.out.println("Ошибка подключения к БД: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}