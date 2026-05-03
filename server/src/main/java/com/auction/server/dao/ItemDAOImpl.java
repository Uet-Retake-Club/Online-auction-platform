package com.auction.server.dao;

import com.auction.server.database.DatabaseConnection;
import com.auction.shared.models.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ItemDAOImpl implements ItemDAO {

    @Override
    public boolean updateCurrentPrice(int itemId, double newPrice) {
        // Lệnh SQL này chứa bí quyết chống Race Condition (Lost Update)
        // Chỉ cập nhật nếu newPrice THỰC SỰ LỚN HƠN current_price hiện tại trong DB
        String sql = "UPDATE items SET current_price = ? WHERE id = ? AND current_price < ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, itemId);
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

    // Bạn sẽ tự triển khai tiếp các hàm getItemById và addItem tương tự nhé!
}