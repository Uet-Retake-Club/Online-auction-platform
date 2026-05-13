package com.auction.server.controllers;

import com.auction.server.controllers.handlers.*;
import com.auction.server.exceptions.AuctionException;
import com.auction.server.exceptions.DatabaseOperationException;
import com.auction.server.exceptions.InvalidBidException;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;

import java.util.HashMap;
import java.util.Map;

public class RequestDispatcher {
    private final Map<MessageType, CommandHandler> handlerRegistry = new HashMap<>();

    public RequestDispatcher() {
        handlerRegistry.put(MessageType.LOGIN, new LoginHandler());
        handlerRegistry.put(MessageType.PLACE_BID, new PlaceBidHandler());
        handlerRegistry.put(MessageType.SETUP_AUTO_BID, new SetupAutoBidHandler());
        handlerRegistry.put(MessageType.GET_STATUS, new GetStatusHandler());
    }

    public Response dispatch(Request request, ClientHandler clientHandler) {
        CommandHandler handler = handlerRegistry.get(request.getType());
        
        if (handler != null) {
            try {
                // Thử thực thi nghiệp vụ
                return handler.handle(request, clientHandler);
                
            } catch (InvalidBidException e) {
                // 1. BẮT LỖI SAI LUẬT (Ví dụ: Giá thấp hơn quy định)
                // -> Trả về trạng thái "FAIL" cho Client biết họ nhập sai
                System.err.println("[Nghiệp vụ] Từ chối Client " + request.getSenderId() + ": " + e.getMessage());
                return new Response(request.getType(), "FAIL", e.getMessage(), null);
                
            } catch (DatabaseOperationException e) {
                // 2. BẮT LỖI CƠ SỞ DỮ LIỆU (Ví dụ: Không lưu được vào SQLite)
                // -> Trả về trạng thái "ERROR" báo lỗi máy chủ
                System.err.println("[Database] Lỗi truy xuất đối với lệnh " + request.getType() + ": " + e.getMessage());
                return new Response(request.getType(), "ERROR", "Hệ thống lưu trữ đang bận. Vui lòng thử lại sau!", null);
                
            } catch (AuctionException e) {
                // 3. BẮT CÁC LỖI NGHIỆP VỤ KHÁC CHƯA PHÂN LOẠI (Lưới vét cho các lỗi con còn lại)
                System.err.println(" [Nghiệp vụ] Lỗi từ Client " + request.getSenderId() + ": " + e.getMessage());
                return new Response(request.getType(), "FAIL", e.getMessage(), null);
                
            } catch (Exception e) {
                // 4. BẮT CÁC LỖI BẤT NGỜ CỦA HỆ THỐNG (NullPointerException, lỗi mạng...)
                System.err.println(" [Hệ thống] Lỗi nghiêm trọng khi xử lý lệnh " + request.getType());
                e.printStackTrace();
                return new Response(request.getType(), "ERROR", "Lỗi máy chủ nội bộ. Vui lòng thử lại sau!", null);
            }
        } else {
            System.out.println(" [DISPATCHER] Không tìm thấy Handler cho lệnh: " + request.getType());
            return new Response(request.getType(), "ERROR", "Lệnh không được hệ thống hỗ trợ!", null);
        }
    }
}