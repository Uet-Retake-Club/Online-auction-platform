package com.auction.server.dao;

import com.auction.server.database.DatabaseConnection;
import com.auction.shared.models.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WatchlistDAOImpl implements WatchlistDAO {
    private final ItemDAO itemDAO = new ItemDAOImpl();

    @Override
    public boolean addToWatchlist(String userId, String itemId) {
        String sql = "INSERT OR IGNORE INTO watchlists (user_id, item_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, itemId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean removeFromWatchlist(String userId, String itemId) {
        String sql = "DELETE FROM watchlists WHERE user_id = ? AND item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, itemId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Item> getWatchlist(String userId) {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT item_id FROM watchlists WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Item item = itemDAO.getItemById(rs.getString("item_id"));
                    if (item != null) items.add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    @Override
    public boolean isInWatchlist(String userId, String itemId) {
        String sql = "SELECT 1 FROM watchlists WHERE user_id = ? AND item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
