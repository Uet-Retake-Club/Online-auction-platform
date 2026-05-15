package com.auction.server.dao;

import com.auction.server.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WalletDAOImpl implements WalletDAO {

    @Override
    public double getBalance(String userId) {
        String sql = "SELECT balance FROM wallets WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    @Override
    public boolean updateBalance(String userId, double amount) {
        String sql = "INSERT INTO wallets (user_id, balance) VALUES (?, ?) " +
                     "ON CONFLICT(user_id) DO UPDATE SET balance = balance + ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setDouble(2, amount);
            pstmt.setDouble(3, amount);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean createTopupRequest(String userId, double amount) {
        String sql = "INSERT INTO topup_requests (id, user_id, amount, status, timestamp) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "TR-" + UUID.randomUUID().toString().substring(0, 8));
            pstmt.setString(2, userId);
            pstmt.setDouble(3, amount);
            pstmt.setString(4, "PENDING");
            pstmt.setLong(5, System.currentTimeMillis());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<TopupRequest> getPendingRequests() {
        List<TopupRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM topup_requests WHERE status = 'PENDING'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                requests.add(new TopupRequest(
                    rs.getString("id"),
                    rs.getString("user_id"),
                    rs.getDouble("amount"),
                    rs.getString("status"),
                    rs.getLong("timestamp")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    @Override
    public boolean updateRequestStatus(String requestId, String status) {
        String sql = "UPDATE topup_requests SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, requestId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
