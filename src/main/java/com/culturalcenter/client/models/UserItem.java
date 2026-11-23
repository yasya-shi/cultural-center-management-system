package com.culturalcenter.client.models;
//Хранит данные одного пользователя
public class UserItem {
    private final int userId;
    private final String login;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String role;
    private final String status;

    public UserItem(int userId, String login, String fullName, String email, String phone, String role, String status) {
        this.userId = userId;
        this.login = login;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
    }

    public int getUserId() { return userId; }
    public String getLogin() { return login; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
}