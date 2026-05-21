package com.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.auction.server.database.DatabaseConnection;
import com.auction.shared.models.BidTransaction;

public class BidTransactionDAOImpl implements BidTransactionDAO {
    @Override
    public boolean addTransaction(BidTransaction tx) {
        String sql = "INSERT INTO bid_transactions (id, item_id, bidder_id, bid_amount, timestamp) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tx.getId());
            pstmt.setString(2, tx.getItemId());
            pstmt.setString(3, tx.getBidderId());
            pstmt.setDouble(4, tx.getBidAmount());
            pstmt.setLong(5, tx.getTimestamp());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }



    
    @Override
    public List<BidTransaction> getHistoryByItem(String itemId) {
        List<BidTransaction> history = new ArrayList<>();
        String sql = "SELECT bt.*, u.username AS bidder_username FROM bid_transactions bt "
                   + "LEFT JOIN users u ON bt.bidder_id = u.id "
                   + "WHERE bt.item_id = ? ORDER BY bt.timestamp ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BidTransaction tx = new BidTransaction(
                        rs.getString("id"),
                        rs.getString("item_id"),
                        rs.getString("bidder_id"),
                        rs.getDouble("bid_amount"),
                        rs.getLong("timestamp")
                    );
                    tx.setBidderUsername(rs.getString("bidder_username"));
                    history.add(tx);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return history;
    }

    @Override
    public List<BidTransaction> getAllTransactions() {
        List<BidTransaction> history = new ArrayList<>();
        String sql = "SELECT bt.*, u.username AS bidder_username FROM bid_transactions bt "
                   + "LEFT JOIN users u ON bt.bidder_id = u.id "
                   + "ORDER BY bt.timestamp DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                BidTransaction tx = new BidTransaction(
                    rs.getString("id"),
                    rs.getString("item_id"),
                    rs.getString("bidder_id"),
                    rs.getDouble("bid_amount"),
                    rs.getLong("timestamp")
                );
                tx.setBidderUsername(rs.getString("bidder_username"));
                history.add(tx);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return history;
    }

    @Override
    public int getTotalBidCount() {
        String sql = "SELECT COUNT(*) FROM bid_transactions";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public double getMaxBidAmount(String userId, String itemId) {
        String sql = "SELECT MAX(bid_amount) FROM bid_transactions WHERE bidder_id = ? AND item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    @Override
    public List<String> getBiddersForItem(String itemId) {
        List<String> bidders = new ArrayList<>();
        String sql = "SELECT DISTINCT bidder_id FROM bid_transactions WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bidders.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bidders;
    }
}