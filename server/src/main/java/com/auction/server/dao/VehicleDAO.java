package com.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeParseException;

import com.auction.server.database.DatabaseConnection;
import com.auction.shared.models.Item;
import com.auction.shared.models.ItemCategory;
import com.auction.shared.models.ItemFactory;
import com.auction.shared.models.Vehicle;

public class VehicleDAO implements ItemDAO {
    
    @Override
    public boolean updateCurrentPrice(String itemId, double newPrice) {
        String sql = "UPDATE items SET current_price = ? WHERE id = ? AND current_price < ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, newPrice);
            pstmt.setString(2, itemId);
            pstmt.setDouble(3, newPrice);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating price: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean addItem(Item item) {
        String sqlItems = "INSERT INTO items (id, name, description, category, start_price, current_price, highest_bidder_id, start_time, end_time, seller_id, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        String sqlVehicles = "INSERT INTO items_vehicles (item_id, brand, model, color) VALUES (?, ?, ?, ?);";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement psmtItems = conn.prepareStatement(sqlItems);
             PreparedStatement psmtVehicles = conn.prepareStatement(sqlVehicles)) {
            
            // Chèn vào bảng items
            psmtItems.setString(1, item.getId());
            psmtItems.setString(2, item.getName());
            psmtItems.setString(3, item.getDescription());
            psmtItems.setString(4, item.getCategory().name());
            psmtItems.setDouble(5, item.getStartingPrice());
            psmtItems.setDouble(6, item.getCurrentHighestBid());
            psmtItems.setString(7, item.getHighestBidderId());
            psmtItems.setLong(8, item.getStartTime());
            psmtItems.setLong(9, item.getEndTime());
            psmtItems.setString(10, item.getSellerId());
            psmtItems.setString(11, item.getStatus());

            int rowsItems = psmtItems.executeUpdate();

            if (rowsItems > 0) {
                psmtVehicles.setString(1, item.getId());
                psmtVehicles.setString(2, item.getBrand());
                psmtVehicles.setString(3, item.getModel());
                psmtVehicles.setString(4, item.getColor());

                int rowsVehicles = psmtVehicles.executeUpdate();
                return rowsVehicles > 0;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Add Item Error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public Item getItemById(String id) {

        String sql = "SELECT * FROM items WHERE id = ?";

        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Vehicle vehicle = new Vehicle();
                vehicle.setId(rs.getString("id"));
                vehicle.setName(rs.getString("name"));
                vehicle.setDescription(rs.getString("description"));
                vehicle.setCategory(ItemCategory.valueOf(rs.getString("category")));
                vehicle.setStartingPrice(rs.getDouble("start_price"));
                vehicle.setCurrentHighestBid(rs.getDouble("current_price"));
                vehicle.setHighestBidderId(rs.getString("highest_bidder_id"));
                vehicle.setStartTime(rs.getLong("start_time"));
                vehicle.setEndTime(rs.getLong("end_time"));
                vehicle.setSellerId(rs.getString("seller_id"));
                vehicle.setStatus(rs.getString("status"));
                vehicle.setBrand(rs.getString("brand"));
                vehicle.setModel(rs.getString("model"));
                vehicle.setColor(rs.getInt("color"));

                return vehicle;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching item: " + e.getMessage());
        }
        return null;
}
