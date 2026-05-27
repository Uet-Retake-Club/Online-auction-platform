package com.auction.server.services;

import com.auction.server.dao.*;
import com.auction.shared.models.*;
import java.util.*;

class AuctionServiceTestFixtures {

    static class FakeItemDAO implements ItemDAO {
        final Map<String, Item> items = new HashMap<>();
        final Map<String, Double> updatedPrices = new HashMap<>();
        final Map<String, String> updatedBidders = new HashMap<>();
        final Map<String, String> updatedStatuses = new HashMap<>();
        int resetCount = 0;
        boolean updatePriceResult = true;
        boolean updateStatusResult = true;
        boolean resetResult = true;

        @Override
        public Item getItemById(String id) {
            return items.get(id);
        }

        @Override
        public boolean addItem(Item item) {
            items.put(item.getId(), item);
            return true;
        }

        @Override
        public boolean updateCurrentPrice(String itemId, double price, String bidderId) {
            updatedPrices.put(itemId, price);
            updatedBidders.put(itemId, bidderId);
            Item item = items.get(itemId);
            if (item != null) {
                item.setCurrentHighestBid(price);
                item.setHighestBidderId(bidderId);
            }
            return updatePriceResult;
        }

        @Override
        public boolean updateStatus(String itemId, String status) {
            updatedStatuses.put(itemId, status);
            Item item = items.get(itemId);
            if (item != null) {
                item.setStatus(status);
            }
            return updateStatusResult;
        }

        @Override
        public Item getFirstOpenItem() {
            return items.values().stream()
                .filter(i -> "OPEN".equals(i.getStatus()))
                .findFirst().orElse(null);
        }

        @Override
        public List<Item> getItemsBySellerId(String sellerId) {
            List<Item> result = new ArrayList<>();
            for (Item item : items.values()) {
                if (sellerId.equals(item.getSellerId())) {
                    result.add(item);
                }
            }
            return result;
        }

        @Override
        public List<Item> getAllItems() {
            return new ArrayList<>(items.values());
        }

        @Override
        public int getActiveAuctionCount() {
            return (int) items.values().stream().filter(i -> "OPEN".equals(i.getStatus())).count();
        }

        @Override
        public boolean resetItemForReauction(String id) {
            resetCount++;
            Item item = items.get(id);
            if (item != null) {
                item.setCurrentHighestBid(item.getStartingPrice());
                item.setHighestBidderId(null);
                item.setStatus("OPEN");
            }
            return resetResult;
        }
    }

    static class FakeBidTransactionDAO implements BidTransactionDAO {
        final List<BidTransaction> transactions = new ArrayList<>();
        final Map<String, List<String>> itemBidders = new HashMap<>();
        boolean addTransactionResult = true;

        @Override
        public boolean addTransaction(BidTransaction tx) {
            transactions.add(tx);
            itemBidders.computeIfAbsent(tx.getItemId(), k -> new ArrayList<>()).add(tx.getBidderId());
            return addTransactionResult;
        }

        @Override
        public List<BidTransaction> getHistoryByItem(String itemId) {
            List<BidTransaction> result = new ArrayList<>();
            for (BidTransaction tx : transactions) {
                if (tx.getItemId().equals(itemId)) {
                    result.add(tx);
                }
            }
            return result;
        }

        @Override
        public List<BidTransaction> getAllTransactions() {
            return new ArrayList<>(transactions);
        }

        @Override
        public int getTotalBidCount() {
            return transactions.size();
        }

        @Override
        public double getMaxBidAmount(String userId, String itemId) {
            return transactions.stream()
                .filter(tx -> tx.getBidderId().equals(userId) && tx.getItemId().equals(itemId))
                .mapToDouble(BidTransaction::getBidAmount)
                .max().orElse(0.0);
        }

        @Override
        public List<String> getBiddersForItem(String itemId) {
            List<String> list = itemBidders.get(itemId);
            if (list == null) return Collections.emptyList();
            List<String> unique = new ArrayList<>();
            for (String bidder : list) {
                if (!unique.contains(bidder)) {
                    unique.add(bidder);
                }
            }
            return unique;
        }

        @Override
        public List<String> getBiddedItemIds(String userId) {
            List<String> unique = new ArrayList<>();
            for (BidTransaction tx : transactions) {
                if (tx.getBidderId().equals(userId) && !unique.contains(tx.getItemId())) {
                    unique.add(tx.getItemId());
                }
            }
            return unique;
        }
    }

    static class FakeWalletDAO implements WalletDAO {
        final Map<String, Double> balances = new HashMap<>();
        final Map<String, List<Double>> balanceUpdates = new HashMap<>();

        @Override
        public double getBalance(String userId) {
            return balances.getOrDefault(userId, 0.0);
        }

        @Override
        public boolean updateBalance(String userId, double amount) {
            double current = getBalance(userId);
            balances.put(userId, current + amount);
            balanceUpdates.computeIfAbsent(userId, k -> new ArrayList<>()).add(amount);
            return true;
        }

        @Override public boolean createTopupRequest(String u, double a) { return true; }
        @Override public List<TopupRequest> getPendingRequests() { return Collections.emptyList(); }
        @Override public List<TopupRequest> getHistory(String u) { return Collections.emptyList(); }
        @Override public boolean updateRequestStatus(String id, String s) { return true; }
    }

    static class FakeUserDAO implements UserDAO {
        final Map<String, User> users = new HashMap<>();

        @Override
        public User getUserById(String id) {
            return users.get(id);
        }

        @Override
        public User getUserByUsername(String username) {
            return users.values().stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
        }

        @Override
        public boolean addUser(User user, String password) {
            users.put(user.getId(), user);
            return true;
        }

        @Override public String authenticateUser(String u, String p) { return null; }
        @Override public List<User> getAllUsers() { return new ArrayList<>(users.values()); }
        @Override public int getUserCount() { return users.size(); }
        @Override public boolean updateUserStatus(String userId, String status) {
            User u = users.get(userId);
            if (u != null) {
                u.setStatus(status);
                return true;
            }
            return false;
        }
    }

    static class FakeInvoiceDAO implements InvoiceDAO {
        final Map<String, Invoice> invoices = new HashMap<>();
        boolean createInvoiceResult = true;
        boolean updateStatusResult = true;

        @Override
        public boolean createInvoice(Invoice invoice) {
            invoices.put(invoice.getId(), invoice);
            return createInvoiceResult;
        }

        @Override
        public Invoice getInvoiceById(String invoiceId) {
            return invoices.get(invoiceId);
        }

        @Override
        public boolean updateInvoiceStatus(String invoiceId, String status) {
            Invoice inv = invoices.get(invoiceId);
            if (inv != null) {
                inv.setStatus(status);
                return updateStatusResult;
            }
            return false;
        }

        @Override
        public List<Invoice> getInvoicesByUserId(String userId) {
            return Collections.emptyList();
        }

        @Override
        public double getTotalRevenue() {
            return 0.0;
        }

        public List<Invoice> getAllInvoices() { return new ArrayList<>(invoices.values()); }
    }

    static class FakeAuctionDAO implements AuctionDAO {
        final Map<String, Auction> auctions = new HashMap<>();

        @Override
        public boolean addAuction(Auction auction) {
            auctions.put(auction.getId(), auction);
            return true;
        }

        @Override
        public Auction getAuctionById(String id) {
            return auctions.get(id);
        }

        @Override
        public List<Auction> getAllAuctions() {
            return new ArrayList<>(auctions.values());
        }
    }
}
