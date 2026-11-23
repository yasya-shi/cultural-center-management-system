package com.culturalcenter.client.models;
//Хранит данные одной заявки для отображения в таблице
public class BookingItem {
    private int bookingId;
    private String centerName;
    private String roomName;
    private String organizerName;
    private String eventDate;
    private String startTime;
    private String endTime;
    private String purpose;
    private String status;
    private String rejectionReason;

    //Перегрузка методов
    // Конструктор для организатора (с centerName)
    public BookingItem(int bookingId, String centerName, String roomName, String eventDate,
                       String startTime, String endTime, String purpose, String status, String rejectionReason) {
        this.bookingId = bookingId;
        this.centerName = centerName;
        this.roomName = roomName;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.organizerName = "";
    }

    // Конструктор для директора (с organizerName)
    public BookingItem(int bookingId, String roomName, String organizerName, String eventDate,
                       String startTime, String endTime, String purpose, String status) {
        this.bookingId = bookingId;
        this.roomName = roomName;
        this.organizerName = organizerName;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.status = status;
        this.centerName = "";
        this.rejectionReason = "";
    }

    // Геттеры
    public int getBookingId() { return bookingId; }
    public String getCenterName() { return centerName; }
    public String getRoomName() { return roomName; }
    public String getOrganizerName() { return organizerName; }
    public String getEventDate() { return eventDate; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getTimeRange() { return startTime + " - " + endTime; }
    public String getPurpose() { return purpose; }
    public String getStatus() {
        switch (status) {
            case "PENDING": return "На рассмотрении";
            case "APPROVED": return "Подтверждена";
            case "REJECTED": return "Отклонена";
            case "CANCELLED": return "Отменена";
            case "COMPLETED": return "Завершено";
            default: return status;
        }
    }
    public String getRawStatus() { return status; }
    public String getRejectionReason() { return rejectionReason != null ? rejectionReason : ""; }
}