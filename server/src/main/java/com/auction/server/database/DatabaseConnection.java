package com.auction.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

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
            
        } catch (SQLException | IOException e) {
            System.err.println("[DATABASE ERROR] Failed to initialize database: " + e.getMessage());
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
}