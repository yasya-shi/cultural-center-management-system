package com.culturalcenter.client.models;
//Хранит данные одной строки отчёта
public class ReportItem {
    private final String roomName;
    private final double hours;
    private final int bookingCount;
    private final double averageRating;

    public ReportItem(String roomName, double hours, int bookingCount, double averageRating) {
        this.roomName = roomName;
        this.hours = hours;
        this.bookingCount = bookingCount;
        this.averageRating = averageRating;
    }

    public String getRoomName() { return roomName; }
    public double getHours() { return hours; }
    public int getBookingCount() { return bookingCount; }
    public double getAverageRating() { return averageRating; }
}