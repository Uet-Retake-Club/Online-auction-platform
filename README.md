# Online-auction-platform
An auction platform using Java, Gradle, JavaFx, Firebase.

## Folder structure

```
Online-auction-platform/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── shared/                       // Chứa các class dùng chung cho cả Client và Server
│   ├── build.gradle
│   └── src/main/java/com/auction/shared/
│       ├── models/               // Entity cơ bản: User, Item, BidTransaction...
│       └── utils/                // Constants, Enums (Trạng thái phiên: OPEN, RUNNING...)
├── server/                       // Xử lý Database, Core Logic, Socket/API
│   ├── build.gradle
│   └── src/main/java/com/auction/server/
│       ├── ServerApplication.java
│       ├── controllers/          // Lắng nghe và xử lý request từ Client
│       ├── services/             // Xử lý logic nghiệp vụ (Auction logic, Anti-sniping)
│       ├── dao/                  // Data Access Object giao tiếp với SQLite
│       ├── database/             // Chứa logic khởi tạo kết nối SQLite
│       └── network/              // Quản lý Socket connections / Event broadcasting
├── client/                       // Ứng dụng JavaFX cho người dùng
│   ├── build.gradle
│   ├── src/main/java/com/auction/client/
│   │   ├── ClientApplication.java
│   │   ├── controllers/          // JavaFX Controllers (điều khiển View)
│   │   ├── network/              // Gửi/nhận dữ liệu từ Server, Firebase Auth
│   │   └── services/             // Client logic (Validate giá, Auto-bidding)
│   └── src/main/resources/com/auction/client/
│       ├── views/                // Các file FXML giao diện
│       │   ├── LoginView.fxml
│       │   ├── AuctionListView.fxml
│       │   ├── ItemDetailView.fxml
│       │   └── RealtimeBiddingView.fxml
│       └── styles/               // Các file CSS thiết kế giao diện
│           ├── main.css
│           └── components.css
└── database/
    └── auction.db                // File database SQLite cục bộ
```

