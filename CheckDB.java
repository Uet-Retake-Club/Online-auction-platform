import java.sql.*;

public class CheckDB {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:database/auction.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            System.out.println("Checking users...");
            printTable(conn, "users");
            System.out.println("\nChecking items...");
            printTable(conn, "items");
            System.out.println("\nChecking auctions...");
            printTable(conn, "auctions");
            System.out.println("\nChecking bid_transactions...");
            printTable(conn, "bid_transactions");
            System.out.println("\nChecking wallets...");
            printTable(conn, "wallets");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void printTable(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT * FROM " + tableName;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(metaData.getColumnName(i) + ": " + rs.getObject(i) + ", ");
                }
                System.out.println();
            }
        }
    }
}
