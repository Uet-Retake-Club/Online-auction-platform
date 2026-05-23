package com.auction.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.imageio.ImageIO;

public class DatabaseConnection {
    private static final String DB_DIR = new java.io.File("database").exists() ? "database" : "../database";
    private static final String URL = "jdbc:sqlite:" + DB_DIR + "/auction.db";

    private DatabaseConnection() {}

    public static void initDatabase() {
        try (Connection connection = getConnection();
             Statement stmt = connection.createStatement()) {
            
            String schemaSql = new String(Files.readAllBytes(Paths.get(DB_DIR + "/schema.sql")));
            String[] statements = schemaSql.split(";");
            
            for (String statement : statements) {
                if (statement.trim().isEmpty()) continue;
                stmt.execute(statement);
            }
            System.out.println("[DATABASE] Successfully initialized from schema.sql");

            // Migration: add email column to users
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN email TEXT UNIQUE");
                System.out.println("[DATABASE] Migration: added 'email' column to users.");
            } catch (SQLException ignored) {}

            // Migration: add status column to users
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN status TEXT DEFAULT 'ACTIVE'");
                System.out.println("[DATABASE] Migration: added 'status' column to users.");
            } catch (SQLException ignored) {}

            // Migration: add image_data column to items
            try {
                stmt.execute("ALTER TABLE items ADD COLUMN image_data BLOB");
                System.out.println("[DATABASE] Migration: added 'image_data' column to items.");
            } catch (SQLException ignored) {}

            // Migration: ensure all users have wallets
            try {
                stmt.execute("INSERT OR IGNORE INTO wallets (user_id, balance) SELECT id, 0.0 FROM users");
                System.out.println("[DATABASE] Migration: ensured all users have wallets.");
            } catch (SQLException e) {
                System.err.println("[DATABASE] Migration Error: " + e.getMessage());
            }

            // Reset seed items to be active in the future if they are expired or finished
            resetSeedItems(connection);

            // Optimize any existing large images in the database to speed up load time
            optimizeExistingImages(connection);
            
        } catch (SQLException | IOException e) {
            System.err.println("[DATABASE ERROR] Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void resetSeedItems(Connection conn) {
        String[] seedIds = {"ITEM-001", "ITEM-002", "ITEM-003", "ITEM-004", "ITEM-005", "ITEM-006", "ITEM-123"};
        long[] durations = {
            30L * 24 * 3600 * 1000, // ITEM-001: 30 days
            1L * 24 * 3600 * 1000,  // ITEM-002: 1 day
            2L * 24 * 3600 * 1000,  // ITEM-003: 2 days
            5L * 24 * 3600 * 1000,  // ITEM-004: 5 days
            7L * 24 * 3600 * 1000,  // ITEM-005: 7 days
            3L * 24 * 3600 * 1000,  // ITEM-006: 3 days
            30L * 24 * 3600 * 1000  // ITEM-123: 30 days
        };

        long now = System.currentTimeMillis();

        try {
            String checkSql = "SELECT COUNT(*) FROM items WHERE id = ? AND (end_time <= ? OR status != 'OPEN')";
            String updateSql = "UPDATE items SET status = 'OPEN', start_time = ?, end_time = ?, current_price = start_price, highest_bidder_id = NULL WHERE id = ?";
            String deleteBidsSql = "DELETE FROM bid_transactions WHERE item_id = ?";
            String deleteInvoicesSql = "DELETE FROM invoices WHERE item_id = ?";
            String deleteAuctionsSql = "DELETE FROM auctions WHERE item_id = ?";

            try (java.sql.PreparedStatement checkPstmt = conn.prepareStatement(checkSql);
                 java.sql.PreparedStatement updatePstmt = conn.prepareStatement(updateSql);
                 java.sql.PreparedStatement deleteBidsPstmt = conn.prepareStatement(deleteBidsSql);
                 java.sql.PreparedStatement deleteInvoicesPstmt = conn.prepareStatement(deleteInvoicesSql);
                 java.sql.PreparedStatement deleteAuctionsPstmt = conn.prepareStatement(deleteAuctionsSql)) {
                
                for (int i = 0; i < seedIds.length; i++) {
                    String id = seedIds[i];
                    long duration = durations[i];

                    checkPstmt.setString(1, id);
                    checkPstmt.setLong(2, now);
                    boolean needsReset = false;
                    try (java.sql.ResultSet rs = checkPstmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            needsReset = true;
                        }
                    }

                    if (needsReset) {
                        System.out.println("[DATABASE] Resetting expired/finished seed item: " + id);
                        
                        // Reset item fields
                        updatePstmt.setLong(1, now);
                        updatePstmt.setLong(2, now + duration);
                        updatePstmt.setString(3, id);
                        updatePstmt.executeUpdate();

                        // Clean up related tables for this seed item
                        deleteBidsPstmt.setString(1, id);
                        deleteBidsPstmt.executeUpdate();

                        deleteInvoicesPstmt.setString(1, id);
                        deleteInvoicesPstmt.executeUpdate();

                        deleteAuctionsPstmt.setString(1, id);
                        deleteAuctionsPstmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to reset seed items: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Connection connection = DriverManager.getConnection(URL);
            
            // Activate Foreign keys (Bắt buộc cho mỗi connection mới)
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            
            System.out.println("Successfully connected to SQL!");
            return connection; // Trả về một kết nối tươi mới cho mỗi luồng
            
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw e; // Ném ngoại lệ SQL để tầng DAO tự xử lý
        }
    }

    private static void optimizeExistingImages(Connection conn) {
        String query = "SELECT id, image_data FROM items WHERE image_data IS NOT NULL";
        String update = "UPDATE items SET image_data = ? WHERE id = ?";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query);
             PreparedStatement pstmt = conn.prepareStatement(update)) {
            
            while (rs.next()) {
                String id = rs.getString("id");
                byte[] imgData = rs.getBytes("image_data");
                if (imgData != null && imgData.length > 150 * 1024) { // > 150 KB
                    System.out.println("[DATABASE] Optimizing large image for item: " + id + " (" + (imgData.length / 1024) + " KB)...");
                    byte[] optimized = resizeImage(imgData, 600, 400);
                    if (optimized != null && optimized.length < imgData.length) {
                        pstmt.setBytes(1, optimized);
                        pstmt.setString(2, id);
                        pstmt.executeUpdate();
                        System.out.println("[DATABASE] Optimized item: " + id + " to " + (optimized.length / 1024) + " KB.");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to optimize existing images: " + e.getMessage());
        }
    }

    private static byte[] resizeImage(byte[] originalBytes, int maxWidth, int maxHeight) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(originalBytes);
            BufferedImage originalImage = ImageIO.read(in);
            if (originalImage == null) {
                return null;
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
                return originalBytes;
            }

            double ratio = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
            int newWidth = (int) (originalWidth * ratio);
            int newHeight = (int) (originalHeight * ratio);

            BufferedImage outputImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = outputImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g2d.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(outputImage, "jpg", out);
            return out.toByteArray();
        } catch (Exception e) {
            System.err.println("Failed to resize image: " + e.getMessage());
            return null;
        }
    }
}