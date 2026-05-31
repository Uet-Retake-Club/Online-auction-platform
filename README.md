# Nền tảng Đấu giá Trực tuyến (Online Auction Platform)


*(Lần cập nhật cuối cùng - Final Update)*

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
- **Cơ sở dữ liệu:** SQLite.

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
- **Server:** `build/server.jar`
- **Client:** `build/client.jar`

## 5. Hướng dẫn chạy chương trình

### Thứ tự thực hiện:
**Lưu ý:** Luôn phải khởi chạy **Server** trước khi chạy **Client**.

#### Bước 1: Khởi chạy Server
Server sẽ lắng nghe các kết nối từ Client tại cổng mặc định (8080).
```bash
# Cách 1: Dùng Maven
mvn -pl server exec:java

# Cách 2: Dùng file JAR (sau khi build)
java -jar build/server.jar
```

#### Bước 2: Khởi chạy Client
Bạn có thể mở nhiều terminal để chạy nhiều Client cùng lúc nhằm thử nghiệm tính năng đấu giá.
```bash
# Cách 1: Dùng Maven
mvn -pl client javafx:run

# Cách 2: Dùng file JAR (sau khi build)
java -jar build/client.jar
```

## 6. Danh sách chức năng đã hoàn thành
- [x] **Đăng ký và Đăng nhập bảo mật:** Xác thực tài khoản dựa trên cơ sở dữ liệu, kiểm tra trạng thái hoạt động của tài khoản và thông báo lỗi chi tiết khi tài khoản bị khóa (Banned).
- [x] **Đấu giá thời gian thực:** Đồng bộ hóa giá hiện tại, thời gian đếm ngược và lịch sử đặt giá (Bid History) tức thì giữa Server và tất cả Client đang kết nối.
- [x] **Lưu trữ dữ liệu bền vững (Persistence):** Lưu trữ toàn bộ dữ liệu (người dùng, vật phẩm, phiên đấu giá, lịch sử đặt giá, ví tiền, hóa đơn) trực tiếp vào cơ sở dữ liệu SQLite, đảm bảo khôi phục nguyên vẹn trạng thái khi Server khởi động lại.
- [x] **Đặt giá thủ công:** Người dùng nhập trực tiếp mức giá thầu mong muốn để tham gia phiên đấu giá một cách trực quan.
- [x] **Auto-Bid (Tự động nâng giá):** Thiết lập mức giá tối đa và bước nhảy của giá thầu để Server tự động đấu giá thay người dùng khi có đối thủ cạnh tranh.
- [x] **Chiến thuật Aggressive Auto-Bid:** Thuật toán tự động nâng giá thông minh, tự động tính toán dựa trên mức giá của đối thủ cộng thêm bước nhảy tối thiểu để duy trì vị thế dẫn đầu.
- [x] **Danh sách theo dõi (Watchlist):** Cho phép người dùng theo dõi và lưu trữ danh sách các sản phẩm đang quan tâm để cập nhật trạng thái nhanh chóng.
- [x] **Ví tiền và Giao dịch (Wallet):** Tích hợp ví điện tử cá nhân để quản lý số dư, tạo yêu cầu nạp tiền (Top-up) chờ Admin phê duyệt và tự động trừ tiền thanh toán hóa đơn khi thắng đấu giá.
- [x] **Quản lý danh sách bán (Seller Dashboard):** Công cụ cho phép người bán đăng tải sản phẩm đấu giá mới với các thuộc tính chi tiết theo danh mục (Electronics, Vehicles, Sports, Fashion, Collectibles, v.v.) và quản lý hóa đơn (Invoice).
- [x] **Trang quản trị (Admin Dashboard):** Giao diện Admin hiển thị số liệu thống kê thời gian thực từ cơ sở dữ liệu (tổng số người dùng, vật phẩm, doanh thu), quản lý khóa/mở khóa tài khoản (Ban/Unban) và hiển thị bảng log đặt giá theo thời gian thực.
- [x] **Giao diện hiện đại:** Thiết kế UI/UX tinh tế, đồng bộ bằng CSS kết hợp với thư viện AtlantaFX hiện đại, hỗ trợ chuyển đổi chủ đề màu sắc (Dark Mode / Light Mode) mượt mà.
- [x] **CI/CD:** Tự động hóa quá trình build và chạy test trên Windows, Linux và macOS thông qua GitHub Actions.

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

## 8. Tài liệu Báo cáo và Video Demo

- **Báo cáo kỹ thuật chi tiết (PDF):** 
```
https://drive.google.com/file/d/1wuxd5ZxvgAfPN6qYWjaocN0hyCsY3lFT/view?usp=sharing
```
- **Video Demo chạy thực tế:** 
```
https://youtu.be/n_uMbL9m76A
```