package com.auction.server.network;

import com.auction.server.services.AuctionManager;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.AutoBidSettings;
import com.auction.shared.models.BidTransaction;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private String clientId = "Unknown"; // Không dùng UUID nữa, sẽ cập nhật khi có lệnh LOGIN
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                Request request = gson.fromJson(inputLine, Request.class);
                processRequest(request);
            }
        } catch (IOException e) {
            System.out.println("🔌 [NETWORK] Client " + clientId + " mất kết nối.");
        } finally {
            if (!clientId.equals("Unknown")) {
                AuctionManager.getInstance().removeClient(clientId);
            }
            closeConnection();
        }
    }

    private void processRequest(Request request) {
        switch (request.getType()) {
            case LOGIN:
                // Nhận ID thật từ giao diện đăng nhập (ví dụ "Hoang", "Tuan",...)
                this.clientId = request.getSenderId();
                AuctionManager.getInstance().registerClient(this.clientId, this);
                sendResponse(new Response(MessageType.LOGIN, "SUCCESS", "Đăng nhập Socket thành công", null));
                break;

            case PLACE_BID:
                BidTransaction bidTx = gson.fromJson(request.getPayload(), BidTransaction.class);
                Response result = AuctionManager.getInstance().processBid(
                        request.getSenderId(), bidTx.getBidAmount(), request.getPayload());
                sendResponse(result);
                break;

            case SETUP_AUTO_BID:
                // Giải mã cài đặt Auto-bid từ UI gửi lên và đưa cho Manager
                AutoBidSettings settings = gson.fromJson(request.getPayload(), AutoBidSettings.class);
                AuctionManager.getInstance().registerAutoBid(settings);
                sendResponse(new Response(MessageType.SETUP_AUTO_BID, "SUCCESS", "Đã lưu cấu hình Auto-Bid", null));
                break;

            default:
                break;
        }
    }

    public void sendResponse(Response response) {
        if (out != null) out.println(gson.toJson(response));
    }

    private void closeConnection() {
        try { if (socket != null) socket.close(); } 
        catch (IOException e) { e.printStackTrace(); }
    }
}