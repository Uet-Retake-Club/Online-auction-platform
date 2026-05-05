package com.auction.shared.dto;

public class Request {
    private MessageType type;
    private String senderId; // Lấy từ UserSession
    private String payload;  // Chứa chuỗi JSON của BidTransaction hoặc AutoBidSettings

    public Request(MessageType type, String senderId, String payload) {
        this.type = type;
        this.senderId = senderId;
        this.payload = payload;
    }

    // Bạn nhớ tạo thêm Getters/Setters nhé
    public MessageType getType() { return type; }
    public String getSenderId() { return senderId; }
    public String getPayload() { return payload; }
}