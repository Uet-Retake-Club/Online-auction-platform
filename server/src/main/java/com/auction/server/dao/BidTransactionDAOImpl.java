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
        String sql = "SELECT * FROM bid_transactions WHERE item_id = ? ORDER BY timestamp ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    history.add(new BidTransaction(
                        rs.getString("id"),
                        rs.getString("item_id"),
                        rs.getString("bidder_id"),
                        rs.getDouble("bid_amount"),
                        rs.getLong("timestamp")
                    ));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return history;
    }
}