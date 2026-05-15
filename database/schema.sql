-- 1. Bảng quản lý người dùng
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT CHECK(role IN ('BIDDER', 'SELLER', 'ADMIN')) NOT NULL
);

-- 2. Bảng Items (với đầy đủ các cột cho đấu giá, khớp mapCommonFields trong ItemDAOImpl.java)
CREATE TABLE IF NOT EXISTS items (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    category TEXT NOT NULL, 
    start_price REAL NOT NULL,
    current_price REAL NOT NULL,
    highest_bidder_id TEXT,
    start_time INTEGER NOT NULL, -- Đổi TEXT -> INTEGER để dùng rs.getLong()
    end_time INTEGER NOT NULL,   -- Đổi TEXT -> INTEGER để dùng rs.getLong()
    seller_id TEXT NOT NULL,
    status TEXT DEFAULT 'OPEN',
    FOREIGN KEY (highest_bidder_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Các bảng con chi tiết (Khớp saveSubCategoryData trong ItemDAOImpl.java)
CREATE TABLE IF NOT EXISTS items_electronics (
    item_id TEXT PRIMARY KEY,
    brand TEXT,
    warranty_period TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS items_vehicles (
    item_id TEXT PRIMARY KEY,
    brand TEXT,
    model TEXT,
    color TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS items_sports (
    item_id TEXT PRIMARY KEY,
    sport TEXT,
    color TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS items_home_garden (
    item_id TEXT PRIMARY KEY,
    color TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS items_fashion (
    item_id TEXT PRIMARY KEY,
    brand TEXT,
    size TEXT,
    color TEXT,
    material TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS items_collectibles (
    item_id TEXT PRIMARY KEY,
    type TEXT,
    rarity TEXT,
    condition TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS items_other (
    item_id TEXT PRIMARY KEY,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- 4. Bảng quản lý phiên đấu giá (Khớp Auction.java)
CREATE TABLE IF NOT EXISTS auctions (
    id TEXT PRIMARY KEY,
    item_id TEXT NOT NULL,
    seller_id TEXT NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 5. Bảng lịch sử đặt giá (Khớp BidTransaction.java)
CREATE TABLE IF NOT EXISTS bid_transactions (
    id TEXT PRIMARY KEY,
    item_id TEXT NOT NULL,
    bidder_id TEXT NOT NULL,
    bid_amount REAL NOT NULL,
    timestamp INTEGER NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 6. Bảng cấu hình tự động đặt giá (Khớp AutoBidSettings.java)
CREATE TABLE IF NOT EXISTS auto_bid_settings (
    bidder_id TEXT NOT NULL,
    auction_id TEXT NOT NULL,
    max_price REAL NOT NULL,
    bid_increment REAL NOT NULL,
    active INTEGER DEFAULT 1,
    aggressive_mode INTEGER DEFAULT 0,
    PRIMARY KEY (bidder_id, auction_id),
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
);

-- 7. Bảng hóa đơn
CREATE TABLE IF NOT EXISTS invoices (
    id TEXT PRIMARY KEY,
    auction_id TEXT NOT NULL,
    item_id TEXT NOT NULL,
    bidder_id TEXT NOT NULL,
    seller_id TEXT NOT NULL,
    final_price REAL NOT NULL,
    timestamp INTEGER NOT NULL,
    status TEXT DEFAULT 'UNPAID',
    FOREIGN KEY (auction_id) REFERENCES auctions(id),
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id),
    FOREIGN KEY (seller_id) REFERENCES users(id)
);

-- Seed Data: Default Users
INSERT OR IGNORE INTO users (id, username, email, password, role) VALUES ('ADMIN-1', 'admin', 'admin@auction.com', 'admin123', 'ADMIN');
INSERT OR IGNORE INTO users (id, username, email, password, role) VALUES ('SELLER-1', 'seller1', 'seller1@auction.com', 'seller123', 'SELLER');
INSERT OR IGNORE INTO users (id, username, email, password, role) VALUES ('SELLER-2', 'seller2', 'seller2@auction.com', 'seller123', 'SELLER');

-- Seed Data: Items (Note: Timestamps converted to integers - placeholders used)
INSERT OR IGNORE INTO items (id, name, description, category, start_price, current_price, start_time, end_time, seller_id, status)
VALUES ('ITEM-001', 'Gaming Laptop RTX 4080', 'Asus ROG Strix with RTX 4080, i9, 32GB DDR5', 'ELECTRONICS', 1240.00, 1240.00, 1715760000000, 1718438400000, 'ADMIN-1', 'OPEN');
INSERT OR IGNORE INTO items_electronics (item_id, brand, warranty_period) VALUES ('ITEM-001', 'Asus ROG', '2 Years');

INSERT OR IGNORE INTO items (id, name, description, category, start_price, current_price, start_time, end_time, seller_id, status)
VALUES ('ITEM-002', 'iPhone 15 Pro Max 256GB', 'Brand new iPhone 15 Pro Max, Natural Titanium, sealed box', 'ELECTRONICS', 780.00, 780.00, 1715760000000, 1715846400000, 'SELLER-1', 'OPEN');
INSERT OR IGNORE INTO items_electronics (item_id, brand, warranty_period) VALUES ('ITEM-002', 'Apple', '1 Year');

INSERT OR IGNORE INTO items (id, name, description, category, start_price, current_price, start_time, end_time, seller_id, status)
VALUES ('ITEM-003', 'Sony WH-1000XM5', 'Industry-leading noise cancelling wireless headphones', 'ELECTRONICS', 190.00, 190.00, 1715760000000, 1715932800000, 'SELLER-1', 'OPEN');
INSERT OR IGNORE INTO items_electronics (item_id, brand, warranty_period) VALUES ('ITEM-003', 'Sony', '1 Year');

INSERT OR IGNORE INTO items (id, name, description, category, start_price, current_price, start_time, end_time, seller_id, status)
VALUES ('ITEM-004', 'MacBook Air M2 13"', 'Apple MacBook Air M2 chip, 8GB RAM, 256GB SSD, Midnight', 'ELECTRONICS', 850.00, 850.00, 1715760000000, 1716192000000, 'SELLER-2', 'OPEN');
INSERT OR IGNORE INTO items_electronics (item_id, brand, warranty_period) VALUES ('ITEM-004', 'Apple', '1 Year');

INSERT OR IGNORE INTO items (id, name, description, category, start_price, current_price, start_time, end_time, seller_id, status)
VALUES ('ITEM-005', 'Mountain Bike 2024 Carbon', '29" Carbon frame mountain bike, Shimano 12-speed drivetrain', 'SPORTS', 320.00, 320.00, 1715760000000, 1716364800000, 'SELLER-2', 'OPEN');
INSERT OR IGNORE INTO items_sports (item_id, sport, color) VALUES ('ITEM-005', 'Cycling', 'Black');

INSERT OR IGNORE INTO items (id, name, description, category, start_price, current_price, start_time, end_time, seller_id, status)
VALUES ('ITEM-006', 'Vintage Rolex Submariner 1969', 'Original 1969 Rolex Submariner in excellent condition. Serviced 2022.', 'COLLECTIBLES', 4500.00, 4500.00, 1715760000000, 1716019200000, 'SELLER-1', 'OPEN');
INSERT OR IGNORE INTO items_collectibles (item_id, type, rarity, condition) VALUES ('ITEM-006', 'Watch', 'Rare', 'Excellent');

INSERT OR IGNORE INTO items (id, name, description, category, start_price, current_price, start_time, end_time, seller_id, status)
VALUES ('ITEM-123', 'Gaming Laptop RTX 4080', 'Asus ROG Strix with RTX 4080', 'ELECTRONICS', 1240.00, 1240.00, 1715760000000, 1718438400000, 'ADMIN-1', 'OPEN');
INSERT OR IGNORE INTO items_electronics (item_id, brand, warranty_period) VALUES ('ITEM-123', 'Asus ROG', '2 Years');
