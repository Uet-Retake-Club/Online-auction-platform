package com.auction.server.controllers;

import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;

public interface CommandHandler {
    Response handle(Request request, ClientHandler clientHandler);
}