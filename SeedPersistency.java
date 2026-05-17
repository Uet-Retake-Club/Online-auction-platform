import java.sql.*;
import java.util.UUID;

public class SeedPersistency {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:database/auction.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();

            // 1. Ensure Wallet for ADMIN-1
            System.out.println("Step 1: Ensuring wallet for ADMIN-1...");
            stmt.execute("INSERT OR IGNORE INTO wallets (user_id, balance) VALUES ('ADMIN-1', 1000000.0)");

            // 2. Create Auction for ITEM-001
            System.out.println("Step 2: Creating Auction for ITEM-001...");
            stmt.execute("INSERT OR IGNORE INTO auctions (id, item_id, seller_id) VALUES ('AUC-ITEM-001', 'ITEM-001', 'ADMIN-1')");

            // 3. Log a Bid Transaction
            System.out.println("Step 3: Logging a bid transaction...");
            String txId = "BID-" + UUID.randomUUID().toString().substring(0, 8);
            double bidAmount = 1500.0;
            long now = System.currentTimeMillis();
            
            String sqlBid = "INSERT INTO bid_transactions (id, item_id, bidder_id, bid_amount, timestamp) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlBid)) {
                pstmt.setString(1, txId);
                pstmt.setString(2, "ITEM-001");
                pstmt.setString(3, "ADMIN-1");
                pstmt.setDouble(4, bidAmount);
                pstmt.setLong(5, now);
                pstmt.executeUpdate();
            }

            // 4. Update Item Current Price to match the bid
            System.out.println("Step 4: Updating item current price...");
            stmt.execute("UPDATE items SET current_price = 1500.0, highest_bidder_id = 'ADMIN-1' WHERE id = 'ITEM-001'");

            conn.commit();
            System.out.println("\n[SUCCESS] Persistency test data seeded successfully!");
            System.out.println("Auction: AUC-ITEM-001");
            System.out.println("Bid Transaction: " + txId + " ($1500.0)");
            System.out.println("Wallet: ADMIN-1 balance checked.");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
