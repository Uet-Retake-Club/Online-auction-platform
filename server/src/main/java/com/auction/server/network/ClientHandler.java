package com.auction.server.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.auction.server.controllers.RequestDispatcher;
import com.auction.server.services.AuctionService;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.google.gson.Gson;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private String clientId = "Unknown";
    private PrintWriter out;
    private BufferedReader in;
    private static final Gson gson = new Gson();
    
    // Nạp sẵn một bộ Tổng đài viên (Dùng chung)
    private static final RequestDispatcher dispatcher = new RequestDispatcher();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
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
            System.out.println("[NETWORK] Client " + clientId + " disconnected.");
        } finally {
            if (!clientId.equals("Unknown")) {
                AuctionService.getInstance().removeClient(clientId);
            }
            closeConnection();
        }
    }

    private void processRequest(Request request) {
        // Auto-rebind client ID if "Unknown" and request has a valid senderId
        if ("Unknown".equals(clientId) && request.getSenderId() != null 
                && !request.getSenderId().isEmpty() && !"Pending".equals(request.getSenderId())) {
            com.auction.server.dao.UserDAO userDAO = new com.auction.server.dao.UserDAOImpl();
            com.auction.shared.models.User user = userDAO.getUserById(request.getSenderId());
            if (user != null && "ACTIVE".equals(user.getStatus())) {
                setClientId(user.getId());
                AuctionService.getInstance().registerClient(user.getId(), this);
                System.out.println("[NETWORK] Auto-bound reconnected client for user: " + user.getId());
            }
        }

        // Quăng Request cho Dispatcher xử lý
        Response response = dispatcher.dispatch(request, this);
        
        // Nếu lệnh có trả về kết quả ngay (như LOGIN, PLACE_BID), gửi về Client
        if (response != null) {
            sendResponse(response);
        }
    }

    public void sendResponse(Response response) {
        if (out != null) {
            out.println(gson.toJson(response));
        }
    }

    public void closeConnection() {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}