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

public class ItemDAOImpl implements ItemDAO {

    @Override
    public boolean updateCurrentPrice(String itemId, double newPrice) {
        // Lệnh SQL này chứa bí quyết chống Race Condition (Lost Update)
        // Chỉ cập nhật nếu newPrice THỰC SỰ LỚN HƠN current_price hiện tại trong DB
        String sql = "UPDATE items SET current_price = ? WHERE id = ? AND current_price < ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, newPrice);
            pstmt.setString(2, itemId);
            pstmt.setDouble(3, newPrice); // Điều kiện chặn
            
            // executeUpdate() trả về số dòng bị ảnh hưởng. 
            // Nếu = 0, nghĩa là giá mới không cao hơn giá hiện tại trong DB -> Cập nhật thất bại.
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật giá: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean addItem(Item item) {
        String sql = "INSERT INTO items (name, description, category, start_price, current_price, highest_bidder_id, start_time, end_time, seller_id, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement psmt = conn.prepareStatement(sql)) {
            psmt.setString(1, item.getName());
            psmt.setString(2, item.getDescription());
            psmt.setString(3, item.getCategory().name());
            psmt.setDouble(4, item.getStartingPrice());
            psmt.setDouble(5, item.getCurrentHighestBid());
            psmt.setString(6, item.getHighestBidderId());
            psmt.setLong(7, item.getStartTime());
            psmt.setLong(8, item.getEndTime());
            psmt.setString(9, item.getSellerId());
            psmt.setString(10, item.getStatus());

            int rowsAffected = psmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Add Item Error" + e.getMessage());
            return false;
        }
    }

    @Override
    public Item getItemById(String id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        
        
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String categoryStr = rs.getString("category");
                    ItemCategory category;
                    try {
                        category = ItemCategory.valueOf(categoryStr.toUpperCase());
                    } catch (IllegalArgumentException | NullPointerException e) {
                        System.err.println("Cảnh báo: Category không hợp lệ hoặc bị null ở ID " + id + ". Đang dùng mặc định.");
                        category = ItemCategory.GENERAL; // Fallback an toàn để không sập Server
                    }
                    
                    Item item = ItemFactory.createItem(category);
                    
                    // 4. Mapping dữ liệu cơ bản
                    item.setId(rs.getString("id"));
                    item.setName(rs.getString("name"));
                    item.setDescription(rs.getString("description"));
                    item.setCategory(category);
                    item.setStartingPrice(rs.getDouble("start_price"));
                    item.setCurrentHighestBid(rs.getDouble("current_price"));
                    item.setHighestBidderId(rs.getString("highest_bidder_id")); // <-- fix
                    item.setSellerId(rs.getString("seller_id"));
                    
                    // 5. Chuyển đổi dữ liệu phức tạp (Ngày tháng và Trạng thái)
                    item.setStartTime(rs.getLong("start_time"));
                    item.setEndTime(rs.getLong("end_time"));
                    
                    item.setStatus(rs.getString("status"));
                    
                    return item;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi SQL nghiêm trọng tại getItemById: " + e.getMessage());
        } catch (DateTimeParseException e) {
            System.err.println("Lỗi định dạng ngày tháng trong DB tại ID " + id + ": " + e.getMessage());
        }
        
        return null;
    }
}