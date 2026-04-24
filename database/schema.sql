-- 1. Bảng quản lý người dùng
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT CHECK(role IN ('BIDDER', 'SELLER', 'ADMIN')) NOT NULL
);

-- 2. Bảng quản lý sản phẩm đấu giá
CREATE TABLE items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    start_price REAL NOT NULL,
    current_price REAL DEFAULT 0,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL, -- Quan trọng cho Anti-sniping
    seller_id INTEGER,
    status TEXT CHECK(status IN ('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED')) DEFAULT 'OPEN',
    FOREIGN KEY (seller_id) REFERENCES users(id)
);

-- 3. Bảng lịch sử đấu giá (Phục vụ Real-time & Visualization)
CREATE TABLE bids (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER,
    bidder_id INTEGER,
    amount REAL NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);