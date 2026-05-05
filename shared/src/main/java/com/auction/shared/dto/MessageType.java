package com.auction.shared.dto;

public enum MessageType {
    PLACE_BID,          // Client gửi yêu cầu đặt giá
    SETUP_AUTO_BID,     // Client gửi cài đặt auto-bid
    BID_RESPONSE,       // Server trả lời kết quả đặt giá (thành công/thất bại)
    NEW_BID_BROADCAST,  // Server thông báo có giá mới cho toàn bộ Client
    ERROR               // Lỗi chung
}