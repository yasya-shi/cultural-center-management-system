package com.culturalcenter.client.models;
//Хранит данные одного культурного центра
public class CenterItem {
    private int centerId;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String directorName;

    // Конструктор для ComboBox (только id и name)
    public CenterItem(int centerId, String name) {
        this.centerId = centerId;
        this.name = name;
    }

    // Конструктор для таблицы админа (все поля)
    public CenterItem(int centerId, String name, String address, String phone, String email, String directorName) {
        this.centerId = centerId;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.directorName = directorName;
    }

    // Геттеры
    public int getCenterId() { return centerId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getDirectorName() { return directorName; }
//переопределение
    @Override
    public String toString() { return name; }
}