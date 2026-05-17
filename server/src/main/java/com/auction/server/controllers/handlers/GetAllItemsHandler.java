package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.ItemDAOImpl;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.Item;
import com.google.gson.Gson;
import java.util.List;

public class GetAllItemsHandler implements CommandHandler {
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private static final Gson gson = new Gson();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        List<Item> items = itemDAO.getAllItems();
        return new Response(MessageType.GET_ALL_ITEMS_RESPONSE, "SUCCESS", "All items fetched", gson.toJson(items));
    }
}
