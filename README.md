# Nền tảng Đấu giá Trực tuyến (Online Auction Platform)

[![CI - Build & Test](https://github.com/Uet-Retake-Club/Online-auction-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Uet-Retake-Club/Online-auction-platform/actions/workflows/ci.yml)

Một ứng dụng đấu giá trực tuyến thời gian thực được xây dựng bằng Java, Maven và JavaFX. Dự án sử dụng kiến trúc đa mô-đun (multi-module) để tách biệt logic giữa Client, Server và các thành phần dùng chung.

## Các tính năng chính

*   **Đấu giá thời gian thực**: Cập nhật giá thầu tức thì giữa tất cả các client thông qua Socket.
*   **Hệ thống Auto-Bid (Đấu giá tự động)**: Người dùng có thể thiết lập mức giá tối đa và bước nhảy để hệ thống tự động nâng giá khi có người khác đặt giá cao hơn.
*   **Chế độ Aggressive**: Chiến thuật đấu giá linh hoạt (nâng giá theo bước nhảy của người dùng hoặc bước nhảy tối thiểu của hệ thống).
*   **Giao diện hiện đại**: Sử dụng JavaFX kết hợp với AtlantaFX (phong cách GitHub/Primer) và Ikonli cho icon.
*   **Đồng bộ trạng thái**: Tự động lấy trạng thái phiên đấu giá hiện tại ngay khi đăng nhập.
*   **CI/CD**: Tích hợp GitHub Actions để tự động kiểm tra code trên Windows, Ubuntu và macOS.

## Cấu trúc thư mục

```text
Online-auction-platform/
├── pom.xml                       # Cấu hình Maven tổng
├── .github/workflows/ci.yml      # Pipeline CI/CD (GitHub Actions)
├── shared/                       # Các Class dùng chung cho cả Client và Server
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/auction/shared/
│       │   ├── models/           # Các đối tượng: User, Item, Auction, BidTransaction...
│       │   └── dto/              # Đối tượng truyền tin (Request, Response, MessageType)
│       └── test/java/com/auction/shared/
│           └── ...               # Unit tests cho models và DTOs
├── server/                       # Xử lý Logic nghiệp vụ và Kết nối Socket
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/auction/server/
│       │   ├── ServerApplication.java # Điểm khởi đầu của Server
│       │   ├── services/         # AuctionManager (Xử lý logic đấu giá & Auto-bid)
│       │   └── network/          # ClientHandler (Quản lý kết nối socket)
│       └── test/java/com/auction/server/
│           └── services/         # Unit tests cho logic phía Server
└── client/                       # Ứng dụng giao diện JavaFX
    ├── pom.xml
    └── src/
        ├── main/java/com/auction/client/
        │   ├── Launcher.java     # Khởi chạy ứng dụng Client
        │   ├── controllers/      # Điều khiển giao diện (Login, Home, Detail)
        │   └── network/          # NetworkClientService (Giao tiếp với Server)
        └── main/resources/       # Giao diện FXML và CSS
```

## Hướng dẫn cài đặt và chạy

### Yêu cầu hệ thống
*   Java JDK 17 trở lên.
*   Maven 3.8+.

### 1. Build dự án
Mở terminal tại thư mục gốc và chạy lệnh sau để biên dịch toàn bộ các mô-đun:

```bash
mvn clean install
```

### 2. Chạy Server
Server cần được khởi chạy trước để lắng nghe kết nối từ các Client (mặc định cổng 8080):

```bash
mvn -pl server exec:java
```

### 3. Chạy Client (JavaFX)
Mở một terminal mới để chạy ứng dụng giao diện:

```bash
mvn -pl client javafx:run
```

## Kiểm thử (Testing)

Dự án có hệ thống unit test đầy đủ cho các thành phần logic. Để chạy tất cả các bài kiểm tra:

```bash
mvn clean verify -pl shared,server -am
```

## CI/CD
Mọi thay đổi khi push hoặc tạo Pull Request lên nhánh main hoặc dev sẽ được tự động build và chạy test trên cả 3 nền tảng: Windows, Ubuntu, macOS để đảm bảo tính ổn định và tương thích.