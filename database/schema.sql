-- 1. Bảng quản lý người dùng
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT CHECK(role IN ('BIDDER', 'SELLER', 'ADMIN')) NOT NULL
);

-- 2. Bảng Items (với đầy đủ các cột cho đấu giá)
CREATE TABLE IF NOT EXISTS items (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    category TEXT NOT NULL, -- Lưu giá trị Enum (ELECTRONICS, SPORTS, VEHICLES...)
    start_price REAL NOT NULL,
    current_price REAL NOT NULL,
    start_time TEXT NOT NULL, -- Định dạng ISO-8601
    end_time TEXT NOT NULL,
    seller_id TEXT, -- Người bán
    highest_bidder_id TEXT, -- Người đặt giá cao nhất hiện tại (cần thiết cho ItemDAOImpl)
    status TEXT DEFAULT 'OPEN',
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (highest_bidder_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS items_electronics (
    item_id TEXT PRIMARY KEY,
    brand TEXT,
    warranty_period TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- 3. Bảng con cho Vehicles
CREATE TABLE IF NOT EXISTS items_vehicles (
    item_id TEXT PRIMARY KEY,
    brand TEXT,
    model TEXT,
    color TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- 4. Bảng con cho Sports (Dựa trên hình bạn gửi)
CREATE TABLE IF NOT EXISTS items_sports (
    item_id TEXT PRIMARY KEY,
    sport TEXT, -- Ví dụ: Football, Tennis
    color TEXT, -- Mới/Cũ
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS items_home_and_garden (
    item_id TEXT PRIMARY KEY,
    color TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- Bảng con cho Fashion
CREATE TABLE IF NOT EXISTS items_fashion (
    item_id TEXT PRIMARY KEY,
    brand TEXT,
    size TEXT,
    color TEXT,
    material TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- Bảng con cho Collectibles
CREATE TABLE IF NOT EXISTS items_collectibles (
    item_id TEXT PRIMARY KEY,
    type TEXT, -- Ví dụ: Coins, Stamps, Figurines
    rarity TEXT, -- Rare, Common, etc.
    condition TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- Bảng con cho Other (không có thuộc tính riêng, chỉ để phân loại)
CREATE TABLE IF NOT EXISTS items_other (
    item_id TEXT PRIMARY KEY,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bids (
    id TEXT PRIMARY KEY,
    item_id TEXT,
    bidder_id TEXT,
    amount REAL NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);

-- Seed Data: Default Users
INSERT OR IGNORE INTO users (id, username, password, role) VALUES ('ADMIN-1', 'admin', 'admin123', 'ADMIN');
INSERT OR IGNORE INTO users (id, username, password, role) VALUES ('SELLER-1', 'seller1', 'seller123', 'SELLER');
INSERT OR IGNORE INTO users (id, username, password, role) VALUES ('SELLER-2', 'seller2', 'seller123', 'SELLER');

-- Seed Data: ELECTRONICS
INSERT OR IGNORE INTO items (id, name, description, category, start_price, current_price, start_time, end_time, seller_id, status)
VALUES ('ITEM-001', 'Gaming Laptop RTX 4080', 'Asus ROG Strix with RTX 4080, i9, 32GB DDR5', 'ELECTRONICS', 1240.00, 1240.00, '2026-05-15T00:00:00Z', '2026-06-15T00:00:00Z', 'ADMIN-1', 'OPEN');
INSERT OR IGNORE INTO items_electronics (item_id, brand, warranty_period) VALUES ('ITEM-001', 'Asus ROG', '2 Years');

INSERT OR IGNORE INTO items (id, name, description, category, start_price, current_price, start_time, end_time, seller_id, status)
VALUES ('ITEM-002', 'iPhone 15 Pro Max 256GB', 'Brand new iPhone 15 Pro Max, Natural Titanium, sealed box', 'ELECTRONICS', 780.00, 780.00, '2026-05-15T00:00:00Z', '2026-05-16T00:00:00Z', 'SELLER-1', 'OPEN');
INSERT OR IGNORE INTO items_electronics (item_id, brand, warranty_period) VALUES ('ITEM-002', 'Apple', '1 Year');

INSERT OR IGNORE INTO items (id, name, description, category, start_price, current_price, start_time, end_time, seller_id, status)
VALUES ('ITEM-003', 'Sony WH-1000XM5', 'Industry-leading noise cancelling wireless headphones', 'ELECTRONICS', 190.00, 190.00, '2026-05-15T00:00:00Z', '2026-05-17T00:00:00Z', 'SELLER-1', 'OPEN');
INSERT OR IGNORE INTO items_electronics (item_id, brand, warranty_period) VALUES ('ITEM-003', 'Sony', '1 Year');

INSERT OR IGNORE INTO items (id, name, description, category, start_price, current_price, start_time, end_time, seller_id, status)
VALUES ('ITEM-004', 'MacBook Air M2 13"', 'Apple MacBook Air M2 chip, 8GB RAM, 256GB SSD, Midnight', 'ELECTRONICS', 850.00, 850.00, '2026-05-15T00:00:00Z', '2026-05-20T00:00:00Z', 'SELLER-2', 'OPEN');
INSERT OR IGNORE INTO items_electronics (item_id, brand, warranty_period) VALUES ('ITEM-004', 'Apple', '1 Year');

-- Seed Data: SPORTS
INSERT OR IGNORE INTO items (id, name, description, category, start_price, current_price, start_time, end_time, seller_id, status)
VALUES ('ITEM-005', 'Mountain Bike 2024 Carbon', '29" Carbon frame mountain bike, Shimano 12-speed drivetrain', 'SPORTS', 320.00, 320.00, '2026-05-15T00:00:00Z', '2026-05-22T00:00:00Z', 'SELLER-2', 'OPEN');
INSERT OR IGNORE INTO items_sports (item_id, sport, color) VALUES ('ITEM-005', 'Cycling', 'Black');

-- Seed Data: COLLECTIBLES
INSERT OR IGNORE INTO items (id, name, description, category, start_price, current_price, start_time, end_time, seller_id, status)
VALUES ('ITEM-006', 'Vintage Rolex Submariner 1969', 'Original 1969 Rolex Submariner in excellent condition. Serviced 2022.', 'COLLECTIBLES', 4500.00, 4500.00, '2026-05-15T00:00:00Z', '2026-05-18T00:00:00Z', 'SELLER-1', 'OPEN');
INSERT OR IGNORE INTO items_collectibles (item_id, type, rarity, condition) VALUES ('ITEM-006', 'Watch', 'Rare', 'Excellent');

-- Backwards-compatible alias so any existing code referencing ITEM-123 still works
INSERT OR IGNORE INTO items (id, name, description, category, start_price, current_price, start_time, end_time, seller_id, status)
VALUES ('ITEM-123', 'Gaming Laptop RTX 4080', 'Asus ROG Strix with RTX 4080', 'ELECTRONICS', 1240.00, 1240.00, '2026-05-15T00:00:00Z', '2026-06-15T00:00:00Z', 'ADMIN-1', 'OPEN');
INSERT OR IGNORE INTO items_electronics (item_id, brand, warranty_period) VALUES ('ITEM-123', 'Asus ROG', '2 Years');