# Online-auction-platform
An auction platform using Java, Gradle, JavaFx.

## Folder structure
```
auction-system/
├── build.gradle
├── settings.gradle
├── common/
│   ├── src/main/java/com/auction/common/
│   │   ├── model/
│   │   ├── network/
│   │   └── util/
│   └── build.gradle
├── server/
│   ├── src/main/java/com/auction/server/
│   │   ├── ServerApplication.java
│   │   ├── controller/
│   │   ├── service/
│   │   ├── dao/
│   │   └── database/
│   ├── src/main/resources/
│   │   └── db/
│   └── build.gradle
└── client/
    ├── src/main/java/com/auction/client/
    │   ├── ClientApplication.java
    │   ├── controller/
    │   ├── service/
    │   └── util/
    ├── src/main/resources/
    │   └── fxml/
    └── build.gradle
```
## Folders' roles

| Thư mục / Module | Vai trò và Chức năng |
| :--- | :--- |
| **common/model** | Nơi định nghĩa các đối tượng thực thể cốt lõi cho toàn bộ hệ thống như `User`, `Item`, `Auction` và `BidTransaction`. |
| **server/controller** | Đóng vai trò điểm tiếp nhận các yêu cầu mạng từ Client và điều phối luồng xử lý nghiệp vụ. |
| **server/dao** | Chứa các lớp tương tác trực tiếp với cơ sở dữ liệu. Các câu lệnh truy vấn SQLite sẽ được thực thi tại đây. |
| **server/database** | Nơi thiết lập cấu hình kết nối theo chuẩn Singleton tới SQLite và lưu trữ file dữ liệu cục bộ. |
| **client/controller** | Quản lý logic trên giao diện, thu thập dữ liệu nhập từ người dùng và gọi các dịch vụ mạng tương ứng. |
| **client/resources/fxml** | Nơi lưu trữ toàn bộ các tệp giao diện tĩnh được thiết kế theo cấu trúc của JavaFX. |
| **Firebase Services** | Các lớp gọi API hoặc SDK của Firebase để xử lý xác thực Gmail có thể được đặt tại `client/service` hoặc `server/service`, tùy thuộc vào luồng kiểm tra token mà nhóm lựa chọn triển khai. |
