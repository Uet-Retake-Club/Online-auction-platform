package com.auction.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:database/auction.db";

    private DatabaseConnection() {}

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
}