package com.auction.server.services.core;

import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private final Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();

    public void registerClient(String clientId, ClientHandler handler) {
        activeClients.put(clientId, handler);
        System.out.println(" [SessionManager] Client registered: " + clientId + " (Total online: " + activeClients.size() + ")");
    }

    public void removeClient(String clientId) {
        activeClients.remove(clientId);
    }
    public void kickClient(String clientId) {
        // Lấy và xóa client khỏi danh sách online
        ClientHandler handler = activeClients.remove(clientId);
        if (handler != null) {
            // 1. Gửi cho họ một thông báo lỗi trước khi ngắt (Dùng cờ BID_ERROR để hiện Toast báo đỏ)
            handler.sendResponse(new Response(MessageType.BID_ERROR, "BANNED", 
                "Tài khoản của bạn đã bị khóa bởi Admin! Kết nối bị ngắt.", null));
            
            // 2. Rút cáp mạng (Ngắt Socket)
            handler.closeConnection();
            System.out.println(" [SessionManager] Đã KICK ép buộc người dùng: " + clientId);
        }
    }

    public void broadcast(Response response) {
        for (ClientHandler client : activeClients.values()) {
            client.sendResponse(response);
        }
    }
    
    public void sendToClient(String clientId, Response response) {
        ClientHandler handler = activeClients.get(clientId);
        if (handler != null) {
            handler.sendResponse(response);
        }
    }

    public void shutdown() {
        System.out.println(" [SessionManager] Disconnecting all clients...");
        broadcast(new Response(MessageType.AUCTION_ENDED, "FAIL", "Server đang bảo trì/tắt đột ngột!", null));
        activeClients.clear();
    }
}