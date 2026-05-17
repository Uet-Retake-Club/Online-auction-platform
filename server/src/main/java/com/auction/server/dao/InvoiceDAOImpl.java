package com.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.auction.server.database.DatabaseConnection;
import com.auction.shared.models.Invoice;

public class InvoiceDAOImpl implements InvoiceDAO {

    @Override
    public boolean createInvoice(Invoice inv) {
        String sql = "INSERT INTO invoices (id, auction_id, item_id, bidder_id, seller_id, final_price, timestamp, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, inv.getId());
            pstmt.setString(2, inv.getAuctionId());
            pstmt.setString(3, inv.getItemId());
            pstmt.setString(4, inv.getBidderId());
            pstmt.setString(5, inv.getSellerId());
            pstmt.setDouble(6, inv.getFinalPrice());
            pstmt.setLong(7, inv.getTimestamp());
            pstmt.setString(8, inv.getStatus());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Invoice getInvoiceById(String id) {
        String sql = "SELECT * FROM invoices WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapInvoice(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public List<Invoice> getInvoicesByUserId(String userId) {
        List<Invoice> list = new ArrayList<>();
        // Lấy cả những hóa đơn mà User là người mua HOẶC người bán
        String sql = "SELECT * FROM invoices WHERE bidder_id = ? OR seller_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapInvoice(rs));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public boolean updateInvoiceStatus(String invoiceId, String status) {
        String sql = "UPDATE invoices SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, invoiceId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private Invoice mapInvoice(ResultSet rs) throws SQLException {
        return new Invoice(
            rs.getString("id"),
            rs.getString("auction_id"),
            rs.getString("item_id"),
            rs.getString("bidder_id"),
            rs.getString("seller_id"),
            rs.getDouble("final_price"),
            rs.getLong("timestamp"),
            rs.getString("status")
        );
    }

    @Override

    public double getTotalRevenue() {
        String sql = "SELECT SUM(final_price) FROM invoices WHERE status = 'PAID'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}