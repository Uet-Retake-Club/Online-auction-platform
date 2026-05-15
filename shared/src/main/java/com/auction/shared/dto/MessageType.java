package com.auction.shared.dto;

/** Enum for message types between client and server. */
public enum MessageType {
  LOGIN,
  LOGIN_SUCCESS,
  LOGIN_FAIL,
  REGISTER,
  REGISTER_SUCCESS,
  REGISTER_FAIL,
  PLACE_BID, // Yêu cầu đặt giá
  SETUP_AUTO_BID, // Cài đặt auto-bid
  BID_SUCCESS, // Server xác nhận đặt giá hợp lệ
  BID_ERROR, // Lỗi (giá thấp, phiên đã đóng...)
  NEW_BID_BROADCAST, // Cập nhật giá realtime cho tất cả (Observer)
  AUCTION_ENDED, // Thông báo kết thúc phiên
  GET_STATUS // Yêu cầu lấy trạng thái hiện tại (giá + người dẫn đầu)
}