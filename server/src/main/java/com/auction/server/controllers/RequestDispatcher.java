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
        handlerRegistry.put(MessageType.REGISTER, new RegisterHandler());
        handlerRegistry.put(MessageType.PLACE_BID, new PlaceBidHandler());
        handlerRegistry.put(MessageType.SETUP_AUTO_BID, new SetupAutoBidHandler());
        handlerRegistry.put(MessageType.GET_STATUS, new GetStatusHandler());
        handlerRegistry.put(MessageType.CREATE_ITEM, new CreateItemHandler());
        handlerRegistry.put(MessageType.GET_SELLER_ITEMS, new GetSellerItemsHandler());
        handlerRegistry.put(MessageType.GET_ALL_ITEMS, new GetAllItemsHandler());
        handlerRegistry.put(MessageType.GET_MY_BIDS, new GetMyBidsHandler());
        handlerRegistry.put(MessageType.WATCHLIST_ADD, new WatchlistHandler());
        handlerRegistry.put(MessageType.WATCHLIST_REMOVE, new WatchlistHandler());
        handlerRegistry.put(MessageType.GET_WATCHLIST, new WatchlistHandler());
        handlerRegistry.put(MessageType.GET_WALLET_BALANCE, new WalletHandler());
        handlerRegistry.put(MessageType.WALLET_TOPUP_REQUEST, new WalletHandler());
        handlerRegistry.put(MessageType.GET_WALLET_HISTORY, new WalletHandler());
        AdminHandler adminHandler = new AdminHandler();
        handlerRegistry.put(MessageType.ADMIN_GET_PENDING_TOPUPS, adminHandler);
        handlerRegistry.put(MessageType.ADMIN_APPROVE_TOPUP, adminHandler);
        handlerRegistry.put(MessageType.ADMIN_REJECT_TOPUP, adminHandler);
        handlerRegistry.put(MessageType.ADMIN_GET_STATS, adminHandler);
        handlerRegistry.put(MessageType.ADMIN_GET_USERS, adminHandler);
        handlerRegistry.put(MessageType.ADMIN_GET_AUCTIONS, adminHandler);
        handlerRegistry.put(MessageType.ADMIN_BAN_USER, adminHandler);
        handlerRegistry.put(MessageType.ADMIN_UNBAN_USER, adminHandler);
        handlerRegistry.put(MessageType.ADMIN_GET_BIDS, adminHandler);

        InvoiceHandler invoiceHandler = new InvoiceHandler();
        handlerRegistry.put(MessageType.valueOf("INVOICE_PAY"), invoiceHandler);
        handlerRegistry.put(MessageType.valueOf("INVOICE_CANCEL"), invoiceHandler);
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
                System.err.println("[Business] Rejected Client " + request.getSenderId() + ": " + e.getMessage());
                return new Response(request.getType(), "FAIL", e.getMessage(), null);
                
            } catch (DatabaseOperationException e) {
                // 2. BẮT LỖI CƠ SỞ DỮ LIỆU (Ví dụ: Không lưu được vào SQLite)
                // -> Trả về trạng thái "ERROR" báo lỗi máy chủ
                System.err.println("[Database] Access error for command " + request.getType() + ": " + e.getMessage());
                return new Response(request.getType(), "ERROR", "Hệ thống lưu trữ đang bận. Vui lòng thử lại sau!", null);
                
            } catch (AuctionException e) {
                // 3. BẮT CÁC LỖI NGHIỆP VỤ KHÁC CHƯA PHÂN LOẠI (Lưới vét cho các lỗi con còn lại)
                System.err.println(" [Business] Error from Client " + request.getSenderId() + ": " + e.getMessage());
                return new Response(request.getType(), "FAIL", e.getMessage(), null);
                
            } catch (Exception e) {
                // 4. BẮT CÁC LỖI BẤT NGỜ CỦA HỆ THỐNG (NullPointerException, lỗi mạng...)
                System.err.println(" [System] Critical error processing command " + request.getType());
                e.printStackTrace();
                return new Response(request.getType(), "ERROR", "Lỗi máy chủ nội bộ. Vui lòng thử lại sau!", null);
            }
        } else {
            System.out.println(" [DISPATCHER] Handler not found for command: " + request.getType());
            return new Response(request.getType(), "ERROR", "Lệnh không được hệ thống hỗ trợ!", null);
        }
    }
}