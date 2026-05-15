-- 1. Bảng quản lý người dùng (Khớp User.java, Admin.java, Bidder.java, Seller.java)
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    email TEXT NOT NULL,
    password TEXT NOT NULL, -- Bổ sung để login
    role TEXT CHECK(role IN ('BIDDER', 'SELLER', 'ADMIN')) NOT NULL
);

-- 2. Bảng vật phẩm tổng quát (Khớp mapCommonFields trong ItemDAOImpl.java)
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
    warranty_period TEXT, -- Khớp với lệnh INSERT trong ItemDAOImpl
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

CREATE TABLE IF NOT EXISTS items_home_garden ( -- Đổi tên cho khớp với code Java
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
    id TEXT PRIMARY KEY, -- Bỏ AUTOINCREMENT vì Java tự tạo ID String
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