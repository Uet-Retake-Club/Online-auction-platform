package com.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.auction.server.database.DatabaseConnection;
import com.auction.shared.models.Collectibles;
import com.auction.shared.models.Electronics;
import com.auction.shared.models.Fashion;
import com.auction.shared.models.HomeAndGarden;
import com.auction.shared.models.Item;
import com.auction.shared.models.ItemCategory;
import com.auction.shared.models.ItemFactory;
import com.auction.shared.models.Sports;
import com.auction.shared.models.Vehicle;

public class ItemDAOImpl implements ItemDAO {

    @Override
    public boolean updateCurrentPrice(String itemId, double newPrice, String bidderId) {
        // SQL: Chỉ cập nhật nếu giá mới (tham số thứ 4) THỰC SỰ lớn hơn giá hiện tại trong DB
        String sql = "UPDATE items SET current_price = ?, highest_bidder_id = ? WHERE id = ? AND ? > current_price";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, newPrice);
            pstmt.setString(2, bidderId);
            pstmt.setString(3, itemId);
            pstmt.setDouble(4, newPrice); // Check lại giá một lần nữa ở tầng DB

            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("Price updated for Item: " + itemId);
                return true;
            } else {
                // Nếu rowsAffected == 0, có nghĩa là ID sai hoặc giá mới thấp hơn giá hiện tại
                System.err.println("Failed to update price: New price is not higher than current price or Item does not exist.");
                return false;
            }
            
        } catch (SQLException e) {
            System.err.println("SQL Error while updating price: " + e.getMessage());
            return false;
        }
    }



    @Override
    public boolean addItem(Item item) {
        String sqlCommon = "INSERT INTO items (id, name, description, category, start_price, current_price, highest_bidder_id, start_time, end_time, seller_id, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sqlCommon)) {
                pstmt.setString(1, item.getId());
                pstmt.setString(2, item.getName());
                pstmt.setString(3, item.getDescription());
                pstmt.setString(4, item.getCategory().name());
                pstmt.setDouble(5, item.getStartingPrice());
                pstmt.setDouble(6, item.getCurrentHighestBid());
                pstmt.setString(7, item.getHighestBidderId());
                pstmt.setLong(8, item.getStartTime());
                pstmt.setLong(9, item.getEndTime());
                pstmt.setString(10, item.getSellerId());
                pstmt.setString(11, item.getStatus());

                if (pstmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            saveSubCategoryData(conn, item);

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            System.err.println("Add item error: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignore) { }
            }
        }
    }

    private void saveSubCategoryData(Connection conn, Item item) throws SQLException {
        if (item instanceof Vehicle) {
            String sql = "INSERT INTO items_vehicles (item_id, brand, model, color) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                Vehicle vehicle = (Vehicle) item;
                pstmt.setString(1, vehicle.getId());
                pstmt.setString(2, vehicle.getBrand());
                pstmt.setString(3, vehicle.getModel());
                pstmt.setString(4, vehicle.getColor());
                pstmt.executeUpdate();
            }
        } else if (item instanceof Electronics) {
            String sql = "INSERT INTO items_electronics (item_id, brand, warranty_period) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                Electronics e = (Electronics) item;
                pstmt.setString(1, e.getId());
                pstmt.setString(2, e.getBrand());
                pstmt.setString(3, e.getWarranty_period());
                pstmt.executeUpdate();
            }
        } else if (item instanceof Sports) {
            String sql = "INSERT INTO items_sports (item_id, sport, color) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                Sports s = (Sports) item;
                pstmt.setString(1, s.getId());
                pstmt.setString(2, s.getSportType());
                pstmt.setString(3, s.getColor());
                pstmt.executeUpdate();
            }
        } else if (item instanceof Fashion) {
            String sql = "INSERT INTO items_fashion (item_id, brand, size, color, material) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                Fashion f = (Fashion) item;
                pstmt.setString(1, f.getId());
                pstmt.setString(2, f.getBrand());
                pstmt.setString(3, f.getSize());
                pstmt.setString(4, f.getColor());
                pstmt.setString(5, f.getMaterial());
                pstmt.executeUpdate();
            }
        } else if (item instanceof Collectibles) {
            String sql = "INSERT INTO items_collectibles (item_id, type, rarity, condition) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                Collectibles c = (Collectibles) item;
                pstmt.setString(1, c.getId());
                pstmt.setString(2, c.getType());
                pstmt.setString(3, c.getRarity());
                pstmt.setString(4, c.getCondition());
                pstmt.executeUpdate();
            }
        } else if (item instanceof HomeAndGarden) {
            String sql = "INSERT INTO items_home_garden (item_id, color) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                HomeAndGarden h = (HomeAndGarden) item;
                pstmt.setString(1, h.getId());
                pstmt.setString(2, h.getColor());
                pstmt.executeUpdate();
            }
        } else {
            String sql = "INSERT INTO items_other (item_id) VALUES (?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, item.getId());
                pstmt.executeUpdate();
            }
        }
    }

    @Override
    public Item getItemById(String id) {
        String sqlBasic = "SELECT * FROM items WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBasic)) {

            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                ItemCategory category = ItemCategory.valueOf(rs.getString("category"));
                return fetchFullItemDetails(conn, id, category);
            }
        } catch (SQLException e) {
            System.err.println("Get item error: " + e.getMessage());
            return null;
        }
    }

    @Override
    public Item getFirstOpenItem() {
        String sqlBasic = "SELECT id, category FROM items WHERE status = 'OPEN' LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBasic);
             ResultSet rs = pstmt.executeQuery()) {

            if (!rs.next()) {
                return null;
            }

            String id = rs.getString("id");
            ItemCategory category = ItemCategory.valueOf(rs.getString("category"));
            return fetchFullItemDetails(conn, id, category);
            
        } catch (SQLException e) {
            System.err.println("Get first open item error: " + e.getMessage());
            return null;
        }
    }

    private Item fetchFullItemDetails(Connection conn, String id, ItemCategory category) throws SQLException {
        String sql;
        switch (category) {
            case VEHICLE:
                sql = "SELECT i.*, v.brand, v.model, v.color FROM items i LEFT JOIN items_vehicles v ON i.id = v.item_id WHERE i.id = ?";
                break;
            case ELECTRONICS:
                sql = "SELECT i.*, e.brand, e.warranty_period FROM items i LEFT JOIN items_electronics e ON i.id = e.item_id WHERE i.id = ?";
                break;
            case COLLECTIBLES:
                sql = "SELECT i.*, c.type, c.rarity, c.condition FROM items i LEFT JOIN items_collectibles c ON i.id = c.item_id WHERE i.id = ?";
                break;
            case HOME_AND_GARDEN:
                sql = "SELECT i.*, h.color FROM items i LEFT JOIN items_home_garden h ON i.id = h.item_id WHERE i.id = ?";
                break;
            case FASHION:
                sql = "SELECT i.*, f.brand, f.size, f.color, f.material FROM items i LEFT JOIN items_fashion f ON i.id = f.item_id WHERE i.id = ?";
                break;
            case SPORTS:
                sql = "SELECT i.*, s.sport, s.color FROM items i LEFT JOIN items_sports s ON i.id = s.item_id WHERE i.id = ?";
                break;
            default:
                sql = "SELECT * FROM items WHERE id = ?";
                break;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Item item = ItemFactory.createItem(category);
                mapCommonFields(item, rs);

                if (item instanceof Vehicle) {
                    mapVehicleFields((Vehicle) item, rs);
                } else if (item instanceof Electronics) {
                    mapElectronicsFields((Electronics) item, rs);
                } else if (item instanceof HomeAndGarden) {
                    mapHomeAndGardenFields((HomeAndGarden) item, rs);
                } else if (item instanceof Sports) {
                    mapSportsFields((Sports) item, rs);
                } else if (item instanceof Fashion) {
                    mapFashionFields((Fashion) item, rs);
                } else if (item instanceof Collectibles) {
                    mapCollectiblesFields((Collectibles) item, rs);
                }
                return item;
            }
        }
        catch (SQLException e) {
            System.err.println("Fetch item details error: " + e.getMessage());
            return null;
        }
    }

    private void mapCommonFields(Item item, ResultSet rs) throws SQLException {
        item.setId(rs.getString("id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setCategory(ItemCategory.valueOf(rs.getString("category")));
        item.setStartingPrice(rs.getDouble("start_price"));
        item.setCurrentHighestBid(rs.getDouble("current_price"));
        item.setHighestBidderId(rs.getString("highest_bidder_id"));
        item.setStartTime(rs.getLong("start_time"));
        item.setEndTime(rs.getLong("end_time"));
        item.setSellerId(rs.getString("seller_id"));
        item.setStatus(rs.getString("status"));
    }

    private void mapVehicleFields(Vehicle vehicle, ResultSet rs) throws SQLException {
        vehicle.setBrand(rs.getString("brand"));
        vehicle.setModel(rs.getString("model"));
        vehicle.setColor(rs.getString("color"));
    }

    private void mapElectronicsFields(Electronics electronics, ResultSet rs) throws SQLException {
        electronics.setBrand(rs.getString("brand"));
        electronics.setWarranty_period(rs.getString("warranty_period"));
    }

    private void mapHomeAndGardenFields(HomeAndGarden h, ResultSet rs) throws SQLException {
        h.setColor(rs.getString("color"));
    }
    
    private void mapSportsFields(Sports s, ResultSet rs) throws SQLException {
        s.setSportType(rs.getString("sport"));
        s.setColor(rs.getString("color"));
    }

    private void mapFashionFields(Fashion f, ResultSet rs) throws SQLException {
        f.setBrand(rs.getString("brand"));
        f.setSize(rs.getString("size"));
        f.setColor(rs.getString("color"));
        f.setMaterial(rs.getString("material"));
    }

    private void mapCollectiblesFields(Collectibles c, ResultSet rs) throws SQLException {
        c.setType(rs.getString("type"));
        c.setRarity(rs.getString("rarity"));
        c.setCondition(rs.getString("condition"));
    }
    
    @Override
    public boolean updateStatus(String itemId, String status) {
        String sql = "UPDATE items SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            pstmt.setString(2, itemId);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println(" [DATABASE] Item " + itemId + " status updated to: " + status);
                return true;
            }
        } catch (SQLException e) {
            System.err.println(" [SQL Error] Cannot update status: " + e.getMessage());
        }
        return false;
    }
    @Override
    public java.util.List<Item> getItemsBySellerId(String sellerId) {
        java.util.List<Item> items = new java.util.ArrayList<>();
        String sql = "SELECT id, category FROM items WHERE seller_id = ? ORDER BY start_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sellerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    ItemCategory category = ItemCategory.valueOf(rs.getString("category"));
                    Item item = fetchFullItemDetails(conn, id, category);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Get items by seller error: " + e.getMessage());
        }
        return items;
    }


    @Override
    public List<Item> getAllItems() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT id, category FROM items WHERE status = 'OPEN'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                ItemCategory category = ItemCategory.valueOf(rs.getString("category"));
                Item item = fetchFullItemDetails(conn, id, category);
                if (item != null) {
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all items: " + e.getMessage());
        }
        return items;
    }

    @Override

    public int getActiveAuctionCount() {
        String sql = "SELECT COUNT(*) FROM items WHERE status = 'OPEN'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}