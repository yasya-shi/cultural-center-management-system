package com.culturalcenter.server.network;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private String operation;  // "LOGIN", "REGISTER", "GET_BOOKINGS" и т.д.
    private String data;       // JSON с данными

    public Request(String operation, String data) {
        this.operation = operation;
        this.data = data;
    }

    public String getOperation() { return operation; }
    public String getData() { return data; }
    public void setOperation(String operation) { this.operation = operation; }
    public void setData(String data) { this.data = data; }
}