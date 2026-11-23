package com.culturalcenter.client.models;
//Хранит данные одного зала
public class RoomItem {
    private int roomId;
    private String name;
    private int capacity;
    private String equipment;
    private double pricePerHour;
    private String status;

    // Конструктор для ComboBox (только id и name)
    public RoomItem(int roomId, String name) {
        this.roomId = roomId;
        this.name = name;
        this.capacity = 0;
        this.equipment = "";
        this.pricePerHour = 0;
        this.status = "OPEN";
    }

    // Конструктор для таблицы залов (полный)
    public RoomItem(int roomId, String name, int capacity, String equipment, double pricePerHour, String status) {
        this.roomId = roomId;
        this.name = name;
        this.capacity = capacity;
        this.equipment = equipment;
        this.pricePerHour = pricePerHour;
        this.status = status;
    }

    // Геттеры
    public int getRoomId() { return roomId; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public String getEquipment() { return equipment; }
    public double getPricePerHour() { return pricePerHour; }
    public String getStatus() {
        return "CLOSED".equals(status) ? "ЗАКРЫТ" : "ОТКРЫТ";
    }

    // Сеттеры (для редактирования)
    public void setName(String name) { this.name = name; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setEquipment(String equipment) { this.equipment = equipment; }
    public void setPricePerHour(double pricePerHour) { this.pricePerHour = pricePerHour; }

    @Override
    public String toString() {
        return name;
    }
}