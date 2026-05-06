package com.auction.server.network;
import com.auction.server.services.AuctionManager;

import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final String clientId;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();

    public ClientHandler(Socket socket) {
        this.socket = socket;
        // Tạm thời cấp một ID ngẫu nhiên cho mỗi Client khi kết nối
        this.clientId = UUID.randomUUID().toString(); 
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Đăng ký bản thân với Manager để nhận Broadcast
            AuctionManager.getInstance().registerClient(clientId, this);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                Request request = gson.fromJson(inputLine, Request.class);
                processRequest(request);
            }
        } catch (IOException e) {
            System.out.println("🔌 [NETWORK] Client " + clientId + " mất kết nối.");
        } finally {
            AuctionManager.getInstance().removeClient(clientId);
            closeConnection();
        }
    }

    private void processRequest(Request request) {
        switch (request.getType()) {
            case PLACE_BID:
                System.out.println("📩 [NETWORK] Đang xử lý yêu cầu PLACE_BID từ: " + request.getSenderId());
                
                // 1. Giải mã Payload JSON thành Object (Trích xuất số tiền đặt giá)
                // Giả định bạn có import com.auction.shared.models.BidTransaction;
                com.auction.shared.models.BidTransaction bidTx = 
                        gson.fromJson(request.getPayload(), com.auction.shared.models.BidTransaction.class);
                
                // 2. Ném sang Manager xử lý và nhận kết quả
                Response result = AuctionManager.getInstance().processBid(
                        request.getSenderId(), 
                        bidTx.getBidAmount(), 
                        request.getPayload()
                );
                
                // 3. Gửi kết quả ngược lại cho Client
                sendResponse(result);
                break;

            case SETUP_AUTO_BID:
                System.out.println("⚙️ [NETWORK] Đang cài đặt AutoBid cho: " + request.getSenderId());
                // Logic auto-bid sẽ thêm vào sau khi test luồng chính thành công
                break;

            default:
                System.out.println("⚠️ [NETWORK] Nhận được lệnh không xác định: " + request.getType());
                break;
        }
    }

    // Hàm public để Manager có thể gọi khi cần Broadcast giá mới
    public void sendResponse(Response response) {
        if (out != null) {
            out.println(gson.toJson(response));
        }
    }

    private void closeConnection() {
        try { if (socket != null) socket.close(); } 
        catch (IOException e) { e.printStackTrace(); }
    }
}