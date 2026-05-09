package com.auction.shared.dto;

/** Represents a request from the client to the server. */
public class Request {
  private MessageType type;
  private String senderId; // Lấy từ UserSession
  private String payload; // Chứa chuỗi JSON của BidTransaction hoặc AutoBidSettings

  /**
   * Constructs a new Request.
   *
   * @param type the type of message
   * @param senderId the identifier of the sender
   * @param payload the message payload
   */
  public Request(MessageType type, String senderId, String payload) {
    this.type = type;
    this.senderId = senderId;
    this.payload = payload;
  }

  public MessageType getType() {
    return type;
  }

  public String getSenderId() {
    return senderId;
  }

  public String getPayload() {
    return payload;
  }
}