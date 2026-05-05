package com.auction.shared.dto;

public class Response {
    private MessageType type;
    private String status;  // "SUCCESS" hoặc "FAIL"
    private String message; // Câu thông báo lỗi (nếu có)
    private String payload; // Dữ liệu đính kèm (nếu có)

    // Khởi tạo, Getters và Setters...
    public MessageType getType() { return type; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getPayload() { return payload; }
}