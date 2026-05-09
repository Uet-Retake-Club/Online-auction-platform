-- 1. Bảng quản lý người dùng
CREATE TABLE users (
    id TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT CHECK(role IN ('BIDDER', 'SELLER', 'ADMIN')) NOT NULL
);



CREATE TABLE items (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    category TEXT NOT NULL, -- Lưu giá trị Enum (ELECTRONICS, SPORTS, VEHICLES...)
    start_price REAL NOT NULL,
    current_price REAL NOT NULL,
    start_time TEXT NOT NULL, -- Định dạng ISO-8601
    end_time TEXT NOT NULL,
    seller_id TEXT, -- Giả định User ID cũng là String
    status TEXT DEFAULT 'OPEN'

    FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE items_electronics (
    item_id TEXT PRIMARY KEY,
    brand TEXT,
    warranty_period TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- 3. Bảng con cho Vehicles
CREATE TABLE items_vehicles (
    item_id TEXT PRIMARY KEY,
    brand TEXT,
    model TEXT,
    color TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- 4. Bảng con cho Sports (Dựa trên hình bạn gửi)
CREATE TABLE items_sports (
    item_id TEXT PRIMARY KEY,
    sport TEXT, -- Ví dụ: Football, Tennis
    color TEXT, -- Mới/Cũ
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE items_home_and_garden (
    item_id TEXT PRIMARY KEY,
    color TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- Bảng con cho Fashion
CREATE TABLE items_fashion (
    item_id TEXT PRIMARY KEY,
    brand TEXT,
    size TEXT,
    color TEXT,
    material TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- Bảng con cho Collectibles
CREATE TABLE items_collectibles (
    item_id TEXT PRIMARY KEY,
    type TEXT, -- Ví dụ: Coins, Stamps, Figurines
    rarity TEXT, -- Rare, Common, etc.
    condition TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- Bảng con cho Other (không có thuộc tính riêng, chỉ để phân loại)
CREATE TABLE items_other (
    item_id TEXT PRIMARY KEY,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE bids (
    id TEXT PRIMARY KEY AUTOINCREMENT,
    item_id TEXT,
    bidder_id TEXT,
    amount REAL NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);