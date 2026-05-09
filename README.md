# Nền tảng Đấu giá Trực tuyến (Online Auction Platform)

[![CI - Build & Test](https://github.com/Uet-Retake-Club/Online-auction-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Uet-Retake-Club/Online-auction-platform/actions/workflows/ci.yml)

## 1. Mô tả bài toán và Phạm vi hệ thống
Dự án xây dựng một hệ thống đấu giá trực tuyến thời gian thực, cho phép nhiều người dùng tham gia đấu giá cùng một lúc. Hệ thống tập trung vào việc đảm bảo tính đồng bộ, hiệu năng cao và trải nghiệm người dùng mượt mà.

**Phạm vi hệ thống:**
- Quản lý các phiên đấu giá với giá hiện tại và thời gian đếm ngược.
- Xử lý đặt giá thầu (Bid) và tự động nâng giá (Auto-bid) thông minh.
- Giao tiếp thời gian thực giữa Server và hàng loạt Client qua Socket.
- Giao diện đồ họa (GUI) trực quan cho người dùng cuối.

## 2. Công nghệ và Yêu cầu cài đặt

### Công nghệ sử dụng:
- **Ngôn ngữ:** Java 17+.
- **Quản lý dự án:** Maven.
- **Giao diện (Client):** JavaFX, AtlantaFX (UI Theme), Ikonli (Icons).
- **Truyền tin:** Socket (TCP), Gson (JSON Serialization).
- **Kiểm thử:** JUnit 5.

### Yêu cầu môi trường:
- **JDK:** Java Development Kit 17 hoặc mới hơn.
- **Maven:** Phiên bản 3.8.0 trở lên.
- **Hệ điều hành:** Hỗ trợ Windows, Linux, macOS.

## 3. Cấu trúc thư mục và Module chính
Dự án được tổ chức theo mô hình Multi-module để dễ dàng quản lý và tái sử dụng code:

```text
Online-auction-platform/
├── shared/           # Chứa các model, DTO và logic dùng chung cho cả Client & Server.
├── server/           # Xử lý logic nghiệp vụ đấu giá, quản lý kết nối và Auto-bid.
├── client/           # Ứng dụng GUI JavaFX cho người dùng tham gia đấu giá.
├── build/            # Nơi lưu trữ các file .jar sau khi build (Fat JAR).
└── pom.xml           # File cấu hình Maven tổng.
```

## 4. Vị trí các file .jar
Sau khi thực hiện lệnh build, các file executable JAR (đã bao gồm đầy đủ thư viện - Fat JAR) sẽ được xuất hiện tại thư mục:
- **Server:** `build/auction-server.jar`
- **Client:** `build/auction-client.jar`

## 5. Hướng dẫn chạy chương trình

### Thứ tự thực hiện:
**Lưu ý:** Luôn phải khởi chạy **Server** trước khi chạy **Client**.

#### Bước 1: Khởi chạy Server
Server sẽ lắng nghe các kết nối từ Client tại cổng mặc định (8080).
```bash
# Cách 1: Dùng Maven
mvn -pl server exec:java

# Cách 2: Dùng file JAR (sau khi build)
java -jar build/auction-server.jar
```

#### Bước 2: Khởi chạy Client
Bạn có thể mở nhiều terminal để chạy nhiều Client cùng lúc nhằm thử nghiệm tính năng đấu giá.
```bash
# Cách 1: Dùng Maven
mvn -pl client javafx:run

# Cách 2: Dùng file JAR (sau khi build)
java -jar build/auction-client.jar
```

## 6. Danh sách chức năng đã hoàn thành
- [x] **Đăng nhập:** Xác thực người dùng và tham gia phiên đấu giá.
- [x] **Đấu giá thời gian thực:** Cập nhật giá và lịch sử thầu tức thì.
- [x] **Đặt giá thủ công:** Người dùng tự nhập giá thầu mới.
- [x] **Auto-Bid (Tự động nâng giá):** Thiết lập mức tối đa và bước nhảy.
- [x] **Chiến thuật Aggressive:** Tự động nâng giá dựa trên giá đối thủ + bước nhảy.
- [x] **Đồng bộ trạng thái:** Tự động lấy giá hiện tại khi Client mới tham gia.
- [x] **Giao diện hiện đại:** Tích hợp CSS theme AtlantaFX cực đẹp.
- [x] **CI/CD:** Tự động build và test trên Windows/Linux/macOS qua GitHub Actions.

---

## 7. Hướng dẫn build file Executable JAR (Fat JAR)
Để đóng gói ứng dụng thành các file `.jar` độc lập có thể chạy ở bất cứ đâu (có cài JRE 17), chúng tôi sử dụng `maven-shade-plugin`.

### Lệnh thực hiện:
Mở terminal tại thư mục gốc của dự án và chạy:
```bash
mvn clean package
```

### Kết quả:
Maven sẽ biên dịch code và đóng gói tất cả các dependencies (thư viện đi kèm như Gson, JavaFX, AtlantaFX...) vào một file duy nhất.
- Kiểm tra thư mục `build/` để thấy các file `auction-server.jar` và `auction-client.jar`.

### Cách chạy file đã build:
```bash
java -jar build/auction-server.jar
java -jar build/auction-client.jar
```

### Checkstyle
```bash
mvn clean install

# checkstyle
mvn checkstyle:check -pl shared

# fixstyle
mvn checkstyle:check -pl shared -Dcheckstyle.fixHarmlessViolations
```