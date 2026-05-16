import java.sql.*;

public class CheckDB {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:database/auction.db";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("--- Table: users ---");
            try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(users)")) {
                while (rs.next()) {
                    System.out.println("Column: " + rs.getString("name") + " (" + rs.getString("type") + ")");
                }
            }

            System.out.println("\n--- Data: users ---");
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
                while (rs.next()) {
                    System.out.println("ID: " + rs.getString("id") + ", User: " + rs.getString("username") + ", Email: " + rs.getString("email"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
