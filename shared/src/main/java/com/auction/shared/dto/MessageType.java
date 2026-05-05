package com.auction.shared.dto;

public enum MessageType {
    PLACE_BID,          // Yêu cầu đặt giá
    SETUP_AUTO_BID,     // Cài đặt auto-bid
    BID_SUCCESS,        // Server xác nhận đặt giá hợp lệ
    BID_ERROR,          // Lỗi (giá thấp, phiên đã đóng...)
    NEW_BID_BROADCAST,  // Cập nhật giá realtime cho tất cả (Observer)
    AUCTION_ENDED       // Thông báo kết thúc phiên
}