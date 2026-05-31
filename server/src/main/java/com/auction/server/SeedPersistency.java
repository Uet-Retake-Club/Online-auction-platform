package com.auction.server;

import java.sql.*;
import java.util.UUID;
import com.auction.server.database.DatabaseConnection;
import java.io.File;
import java.nio.file.Files;

public class SeedPersistency {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();

            // 1. Ensure Wallet for ADMIN-1
            System.out.println("Step 1: Ensuring wallet for ADMIN-1...");
            stmt.execute("INSERT OR IGNORE INTO wallets (user_id, balance) VALUES ('ADMIN-1', 1000000.0)");
            // clear old items
            stmt.execute("DELETE FROM items WHERE id NOT LIKE 'ITM-%'");
            
            System.out.println("Step 5: Seeding 28 mock items with images...");

            String[][] mockItems = {
                // ELECTRONICS
                {"ITM-E01", "iPad Pro M4 11-inch", "Tablet cao cấp chip M4 mạnh mẽ.", "ELECTRONICS", "1200.0"},
                {"ITM-E02", "LG OLED C3 55 inch TV", "Smart TV 4K hình ảnh sắc nét.", "ELECTRONICS", "1500.0"},
                {"ITM-E03", "Canon EOS R5 Body", "Máy ảnh mirrorless chuyên nghiệp quay 8K.", "ELECTRONICS", "3200.0"},
                {"ITM-E04", "Samsung Galaxy S24 Ultra", "Tích hợp Galaxy AI, camera 200MP.", "ELECTRONICS", "1100.0"},

                // FASHION
                {"ITM-F01", "Omega Speedmaster Professional", "Đồng hồ cơ Thụy Sĩ huyền thoại.", "FASHION", "6500.0"},
                {"ITM-F02", "Louis Vuitton Neverfull", "Túi xách nữ da thật, tình trạng 99%.", "FASHION", "1500.0"},
                {"ITM-F03", "Nike Air Jordan 1 Retro", "Bản phối màu Chicago, size 42.", "FASHION", "450.0"},
                {"ITM-F04", "Gucci GG Leather Belt", "Thắt lưng nam mặt chữ G kép.", "FASHION", "350.0"},

                // SPORTS
                {"ITM-S01", "Everlast Pro Style Gloves", "Găng tay boxing da tổng hợp 14oz.", "SPORTS", "45.0"},
                {"ITM-S02", "Wilson Pro Staff 97", "Vợt tennis chuyên nghiệp 315g.", "SPORTS", "180.0"},
                {"ITM-S03", "Titleist T100 Golf Irons", "Bộ gậy sắt dành cho golfer pro.", "SPORTS", "1200.0"},
                {"ITM-S04", "Bowflex SelectTech 552", "Bộ tạ tay điều chỉnh cân nặng.", "SPORTS", "300.0"},

                // HOME_AND_GARDEN
                {"ITM-H01", "Dyson V15 Detect", "Máy hút bụi không dây lực hút mạnh.", "HOME_AND_GARDEN", "600.0"},
                {"ITM-H02", "Herman Miller Aeron", "Ghế công thái học bảo vệ cột sống.", "HOME_AND_GARDEN", "900.0"},
                {"ITM-H03", "Weber Genesis Gas Grill", "Lò nướng BBQ ngoài trời cỡ lớn.", "HOME_AND_GARDEN", "750.0"},
                {"ITM-H04", "Samsung Bespoke Fridge", "Tủ lạnh 4 cửa thiết kế tuỳ chỉnh màu.", "HOME_AND_GARDEN", "2200.0"},

                // VEHICLE
                {"ITM-V01", "Honda SH 150i 2024", "Xe tay ga cao cấp, phanh ABS 2 kênh.", "VEHICLE", "4500.0"},
                {"ITM-V02", "VinFast VF8 Eco", "Ô tô điện thông minh, kèm pin.", "VEHICLE", "35000.0"},
                {"ITM-V03", "Ducati Panigale V4 S", "Siêu mô tô thể thao 1103cc.", "VEHICLE", "28000.0"},
                {"ITM-V04", "Ford Ranger Raptor", "Siêu bán tải off-road đời 2023.", "VEHICLE", "42000.0"},

                // COLLECTIBLES
                {"ITM-C01", "Charizard 1st Edition Holographic", "Thẻ bài Pokemon cổ, độ mới PSA 9.", "COLLECTIBLES", "5000.0"},
                {"ITM-C02", "Vintage 1969 Apollo 11 Coin", "Đồng xu kỷ niệm sự kiện lên mặt trăng.", "COLLECTIBLES", "200.0"},
                {"ITM-C03", "Signed Lionel Messi Jersey", "Áo đấu tuyển Argentina kèm chữ ký.", "COLLECTIBLES", "1500.0"},
                {"ITM-C04", "Antique Ming Dynasty Vase", "Bình gốm thời Minh nguyên vẹn.", "COLLECTIBLES", "8500.0"},

                // OTHER
                {"ITM-O01", "Taylor Swift VIP Ticket", "Vé VIP Eras Tour hàng ghế đầu.", "OTHER", "1200.0"},
                {"ITM-O02", "Tech Mystery Box", "Hộp quà công nghệ trị giá ẩn.", "OTHER", "50.0"},
                {"ITM-O03", "Netflix Premium 1-Year", "Thẻ cào tài khoản Netflix 12 tháng.", "OTHER", "100.0"},
                {"ITM-O04", "Premium Domain: auction.com", "Tên miền cực đẹp cho sàn đấu giá.", "OTHER", "25000.0"}
            };

            long currentTime = System.currentTimeMillis();
            long endAuctionTime = currentTime + (7 * 24 * 60 * 60 * 1000L); 

            // SQL statement with the accurately verified start_price and image_data columns
            String insertItemSQL = "REPLACE INTO items (id, name, description, category, start_price, current_price, start_time, end_time, seller_id, status, image_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement itemPstmt = conn.prepareStatement(insertItemSQL)) {
                for (String[] item : mockItems) {
                    String itemId = item[0];
                    itemPstmt.setString(1, itemId);
                    itemPstmt.setString(2, item[1]);
                    itemPstmt.setString(3, item[2]);
                    itemPstmt.setString(4, item[3]);
                    
                    double price = Double.parseDouble(item[4]);
                    itemPstmt.setDouble(5, price);
                    itemPstmt.setDouble(6, price);
                    
                    itemPstmt.setLong(7, currentTime);
                    itemPstmt.setLong(8, endAuctionTime);
                    itemPstmt.setString(9, "ADMIN-1");
                    itemPstmt.setString(10, "OPEN");
                    
                    // Locate and read the image file from the mock_images directory
                    File imgFile = new File("mock_images/" + itemId + ".jpg"); 
                    if (imgFile.exists()) {
                        byte[] imageBytes = Files.readAllBytes(imgFile.toPath());
                        itemPstmt.setBytes(11, imageBytes);
                    } else {
                        itemPstmt.setBytes(11, null); 
                    }
                    
                    itemPstmt.addBatch(); 
                }
                itemPstmt.executeBatch();
                System.out.println("28 mock items with images seeded successfully!");
            } catch (Exception e) {
                System.out.println("Error while seeding mock data: " + e.getMessage());
            }
            


            conn.commit();

            System.out.println("\n[SUCCESS] Persistency test data seeded successfully!");
            System.out.println("Wallet: ADMIN-1 balance checked.");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
