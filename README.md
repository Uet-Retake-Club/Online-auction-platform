# Online-auction-platform

[![CI - Build & Test](https://github.com/Uet-Retake-Club/Online-auction-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Uet-Retake-Club/Online-auction-platform/actions/workflows/ci.yml)

An auction platform using Java, Maven, JavaFx.

## Folder structure

```text
Online-auction-platform/
├── pom.xml
├── .github/workflows/ci.yml     // CI/CD pipeline (GitHub Actions)
├── shared/                       // Chứa các class dùng chung cho cả Client và Server
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/auction/shared/
│       │   ├── models/           // Entity cơ bản: User, Item, BidTransaction...
│       │   └── dto/              // Request, Response, MessageType
│       └── test/java/com/auction/shared/
│           ├── models/           // Unit tests cho models
│           └── dto/              // Unit tests cho DTOs
├── server/                       // Xử lý Core Logic, Socket
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/auction/server/
│       │   ├── ServerApplication.java
│       │   ├── services/         // Xử lý logic nghiệp vụ (AuctionManager)
│       │   └── network/          // Quản lý Socket connections (ClientHandler)
│       └── test/java/com/auction/server/
│           └── services/         // Unit tests cho AuctionManager
└── client/                       // Ứng dụng JavaFX cho người dùng
    ├── pom.xml
    └── src/main/java/com/auction/client/
        ├── Launcher.java
        ├── controllers/          // JavaFX Controllers
        └── network/              // Gửi/nhận dữ liệu từ Server
```

## CI/CD

Dự án sử dụng **GitHub Actions** để tự động build và test mỗi khi:
- Push lên branch `main` hoặc `dev`
- Tạo Pull Request vào `main` hoặc `dev`

Pipeline chạy trên **3 hệ điều hành** (Ubuntu, Windows, macOS) để đảm bảo tính tương thích đa nền tảng.

> **Lưu ý:** Module `client` (JavaFX) không chạy test trên CI vì cần display server. Chỉ `shared` và `server` được test tự động.

## Building the Maven Project

Build toàn bộ project:

```bash
mvn clean install
```

## Running Tests

Chạy test cho modules `shared` và `server`:

```bash
mvn clean verify -pl shared,server -am
```

Chạy test cho một module cụ thể:

```bash
mvn test -pl shared
mvn test -pl server
```

## Running the Application

### Server

```bash
mvn -pl server exec:java
```

### Client (JavaFX)

```bash
mvn -pl client javafx:run
```