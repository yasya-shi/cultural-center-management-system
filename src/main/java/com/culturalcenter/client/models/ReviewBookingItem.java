package com.culturalcenter.client.models;

public class ReviewBookingItem {
    private int bookingId;
    private String centerName;
    private String roomName;
    private String eventDate;

    public ReviewBookingItem(int bookingId, String centerName, String roomName, String eventDate) {
        this.bookingId = bookingId;
        this.centerName = centerName;
        this.roomName = roomName;
        this.eventDate = eventDate;
    }

    public int getBookingId() { return bookingId; }
    public String getCenterName() { return centerName; }
    public String getRoomName() { return roomName; }
    public String getEventDate() { return eventDate; }
    @Override public String toString() { return centerName + " - " + roomName + " (" + eventDate + ")"; }
}