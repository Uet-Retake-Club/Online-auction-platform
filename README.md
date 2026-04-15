# Online-auction-platform

An auction platform using Java, Gradle, JavaFx, Firebase.

## Folder structure

```text
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

## Building the Maven Project

First, build the entire project to compile and package all modules:

``` text
mvn clean install
```

This will:

- Compile all source code
- Run tests (if any)
- Package JARs for each module
- Running the Client (JavaFX Application)
- The client is a JavaFX app. Run it with:

``` text
mvn -pl client javafx:run
```

This uses the javafx-maven-plugin configured in pom.xml.
Running the Server
The server is a console app. Run it with:

``` text
mvn -pl server exec:java
```

This uses the exec-maven-plugin configured in pom.xml.