package com.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.auction.server.database.DatabaseConnection;
import com.auction.shared.models.Auction;
import com.auction.shared.models.Item;
import com.auction.shared.models.Seller;

public class AuctionDAOImpl implements AuctionDAO {
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();

    @Override
    public boolean addAuction(Auction auction) {
        String sql = "INSERT INTO auctions (id, item_id, seller_id) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auction.getId());
            pstmt.setString(2, auction.getItem().getId());
            pstmt.setString(3, auction.getSeller().getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private Seller getSellerFromUser(String userId) {
        com.auction.shared.models.User user = userDAO.getUserById(userId);
        if (user == null) return null;
        if (user instanceof Seller) return (Seller) user;
        return new Seller(user.getId(), user.getUsername(), user.getEmail(), user.getStatus());
    }

    @Override
    public Auction getAuctionById(String id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Item item = itemDAO.getItemById(rs.getString("item_id"));
                    Seller seller = getSellerFromUser(rs.getString("seller_id"));
                    return new Auction(id, item, seller);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public List<Auction> getAllAuctions() {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Item item = itemDAO.getItemById(rs.getString("item_id"));
                Seller seller = getSellerFromUser(rs.getString("seller_id"));
                list.add(new Auction(rs.getString("id"), item, seller));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}