package com.auction.server.controllers.handlers;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.WatchlistDAO;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.dto.UserBidDTO;
import com.auction.shared.models.ItemCategory;
import com.auction.shared.models.Electronics;
import com.auction.shared.models.Item;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetMyBidsHandlerTest {

    private static class MockClientHandler extends ClientHandler {
        private final String mockClientId;
        public MockClientHandler(String id) {
            super(null);
            this.mockClientId = id;
        }
        @Override
        public String getClientId() {
            return mockClientId;
        }
    }

    @Test
    public void testHandleNotAuthenticated() {
        GetMyBidsHandler handler = new GetMyBidsHandler();
        MockClientHandler mockClient = new MockClientHandler("Unknown");
        Request req = new Request(MessageType.GET_MY_BIDS, "Unknown", "");
        
        Response resp = handler.handle(req, mockClient);
        assertEquals(MessageType.MY_BIDS_RESPONSE, resp.getType());
        assertEquals("FAIL", resp.getStatus());
        assertEquals("[]", resp.getPayload());
    }

    @Test
    public void testHandleAuthenticated() throws Exception {
        GetMyBidsHandler handler = new GetMyBidsHandler();
        MockClientHandler mockClient = new MockClientHandler("user1");
        
        // Mock the DAOs
        ItemDAO mockItemDAO = new ItemDAO() {
            @Override
            public Item getItemById(String id) {
                if ("item1".equals(id)) {
                    Electronics e = new Electronics("item1", "Item One", "Desc One", 100.0, 2000000L, 3000000L, "seller1", "Brand", "Model");
                    e.setCurrentHighestBid(150.0);
                    e.setHighestBidderId("user1");
                    return e;
                }
                return null;
            }
            @Override public boolean addItem(Item item) { return true; }
            @Override public boolean updateCurrentPrice(String itemId, double newPrice, String bidderId) { return true; }
            @Override public boolean updateStatus(String itemId, String status) { return true; }
            @Override public Item getFirstOpenItem() { return null; }
            @Override public List<Item> getItemsBySellerId(String sellerId) { return Collections.emptyList(); }
            @Override public List<Item> getAllItems() { return Collections.emptyList(); }
            @Override public int getActiveAuctionCount() { return 0; }
            @Override public boolean resetItemForReauction(String itemId) { return true; }
        };

        BidTransactionDAO mockBidDAO = new BidTransactionDAO() {
            @Override
            public List<String> getBiddedItemIds(String userId) {
                List<String> list = new ArrayList<>();
                list.add("item1");
                return list;
            }
            @Override public boolean addTransaction(com.auction.shared.models.BidTransaction tx) { return true; }
            @Override public List<com.auction.shared.models.BidTransaction> getHistoryByItem(String itemId) { return Collections.emptyList(); }
            @Override public List<com.auction.shared.models.BidTransaction> getAllTransactions() { return Collections.emptyList(); }
            @Override public int getTotalBidCount() { return 0; }
            @Override public double getMaxBidAmount(String userId, String itemId) { return 120.0; }
            @Override public List<String> getBiddersForItem(String itemId) { return Collections.emptyList(); }
        };

        WatchlistDAO mockWatchlistDAO = new WatchlistDAO() {
            @Override
            public List<Item> getWatchlist(String userId) {
                return Collections.emptyList();
            }
            @Override public boolean addToWatchlist(String userId, String itemId) { return true; }
            @Override public boolean removeFromWatchlist(String userId, String itemId) { return true; }
            @Override public boolean isInWatchlist(String userId, String itemId) { return false; }
        };

        java.lang.reflect.Field itemDAOField = GetMyBidsHandler.class.getDeclaredField("itemDAO");
        itemDAOField.setAccessible(true);
        itemDAOField.set(handler, mockItemDAO);

        java.lang.reflect.Field bidDAOField = GetMyBidsHandler.class.getDeclaredField("bidDAO");
        bidDAOField.setAccessible(true);
        bidDAOField.set(handler, mockBidDAO);

        java.lang.reflect.Field watchlistDAOField = GetMyBidsHandler.class.getDeclaredField("watchlistDAO");
        watchlistDAOField.setAccessible(true);
        watchlistDAOField.set(handler, mockWatchlistDAO);

        Request req = new Request(MessageType.GET_MY_BIDS, "user1", "");
        Response resp = handler.handle(req, mockClient);

        assertEquals(MessageType.MY_BIDS_RESPONSE, resp.getType());
        assertEquals("SUCCESS", resp.getStatus());

        UserBidDTO[] bids = new Gson().fromJson(resp.getPayload(), UserBidDTO[].class);
        assertEquals(1, bids.length);
        assertEquals("item1", bids[0].getItemId());
        assertEquals("Item One", bids[0].getName());
        assertEquals("ELECTRONICS", bids[0].getCategory());
        assertEquals(120.0, bids[0].getMyHighestBid(), 0.001);
        assertFalse(bids[0].isWatchlisted());
    }
}
