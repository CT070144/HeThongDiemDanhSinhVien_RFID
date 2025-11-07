# RFID Desktop Client

Ứng dụng desktop được viết bằng Java Swing đóng vai trò là client cho hệ thống điểm danh RFID hiện có (Spring Boot backend + cơ sở dữ liệu đã triển khai). Phần mềm này tái hiện các chức năng của frontend ReactJS trước đây, đồng thời tận dụng lại toàn bộ REST API và WebSocket mà backend đang cung cấp.

## 1. Tổng quan kiến trúc

- **Công nghệ**: Java 17, Swing, Maven.
- **Thư viện chính**:
  - `OkHttp` + `Jackson` để gọi REST API và ánh xạ JSON ↔ DTO.
  - `socket.io-client` để kết nối realtime với Socket.IO server của backend.
  - `XChart` để hiển thị các biểu đồ thống kê.
  - `FlatLaf` để cải thiện giao diện Swing.
- **Cấu trúc gói**:
  - `com.rfid.desktop`: lớp khởi động (`App`).
  - `com.rfid.desktop.ui`: toàn bộ màn hình (Swing `JPanel`/`JFrame`), gồm `MainFrame`, `LoginPanel`, `NavigationPanel`, `DashboardPanel`, `StudentManagementPanel`, `PlaceholderPanel`.
  - `com.rfid.desktop.service`: các lớp dịch vụ dùng chung (`ApiClient`, `AuthService`, `StudentService`, `AttendanceService`, `DeviceService`, `LopHocPhanService`, `SessionManager`, `ApplicationContext`).
  - `com.rfid.desktop.model`: các DTO khớp với JSON trả về từ backend (`UserAccount`, `AuthResponse`, `Student`, `AttendanceRecord`, `Device`, `LopHocPhan`, `RfidEvent`).
  - `com.rfid.desktop.websocket`: lớp `WebSocketService` quản lý kết nối Socket.IO.
  - `com.rfid.desktop.chart`: tiện ích tạo biểu đồ (`AttendanceChartFactory`).

## 2. Luồng hoạt động chính

1. **Khởi chạy ứng dụng** (`App.main`) cấu hình FlatLaf và tạo `MainFrame`.
2. `MainFrame` hiển thị `LoginPanel`; người dùng đăng nhập qua API `/api/auth/login`.
3. Khi đăng nhập thành công, token & thông tin user được lưu tại `SessionManager`, đồng thời các navigation panel được khởi tạo.
4. `DashboardPanel` tải dữ liệu tổng quan (sinh viên, điểm danh hôm nay, RFIDs chưa xử lý, thiết bị) thông qua các dịch vụ REST, sau đó hiển thị bằng bảng `JTable` và biểu đồ `XChart`. WebSocket (`update-attendance`) tự động làm mới dữ liệu realtime.
5. `StudentManagementPanel` cung cấp CRUD sinh viên, tìm kiếm, lọc theo lớp học phần, import Excel, quét RFID. Việc polling RFID sử dụng API `attendance/unprocessed-rfids` tương tự frontend cũ.
6. Các màn hình còn lại (`Attendance`, `Classes`, `Devices`) hiện đang là placeholder và có thể được mở rộng sau, tái sử dụng dịch vụ đã có.

## 3. Thiết lập môi trường

- **Yêu cầu**:
  - JDK 17 (hoặc cao hơn nhưng tương thích với Maven compiler target 17).
  - Maven 3.8+ (nếu dùng wrapper thì tùy chỉnh thêm).
  - Backend Spring Boot và Socket.IO server đang chạy (mặc định: REST `http://localhost:8080/api`, Socket.IO `http://localhost:8099`).

- **Biến cấu hình tùy chọn**: có thể override URL bằng các biến môi trường hoặc thuộc tính JVM:
  - `RFID_API_BASE_URL` hoặc `-Drfid.api.base-url=...`
  - `RFID_SOCKET_URL` hoặc `-Drfid.socket.url=...`

## 4. Build & chạy

```bash
cd DesktopApp
mvn clean package
```

Maven sẽ tạo file jar trong `target/rfid-desktop-app-0.1.0-SNAPSHOT.jar`. Để chạy:

```bash
java -jar target/rfid-desktop-app-0.1.0-SNAPSHOT.jar
```

Hoặc chạy trực tiếp class `com.rfid.desktop.App` bằng IDE (IntelliJ/VS Code) với JDK 17.

> **Lưu ý**: nếu máy chưa cài Maven, cần cài đặt và thêm vào `PATH`. Trong trường hợp không thể cài Maven, có thể sử dụng IntelliJ để import dự án Maven và chạy trực tiếp.

## 5. Kịch bản kiểm thử gợi ý

1. **Đăng nhập**: sử dụng tài khoản admin hiện có trên backend.
2. **Dashboard**: kiểm tra số lượng sinh viên, bảng điểm danh, các biểu đồ, và realtime update (quẹt thẻ -> websocket update).
3. **Quản lý sinh viên**: tạo, sửa, xóa sinh viên; lọc theo lớp học phần; import Excel mẫu và quét RFID (polling).
4. **Lỗi kết nối**: tắt backend và xác nhận ứng dụng thông báo lỗi phù hợp khi gọi API.

## 6. Mở rộng trong tương lai

- Hoàn thiện màn hình Lịch sử điểm danh, Lớp học phần, Thiết bị.
- Thêm hệ thống role-based UI (ẩn/bật tính năng theo quyền).
- Đóng gói thành installer (MSI/EXE) với Java runtime (jlink/jpackage).
- Ghi log chi tiết và hiển thị thông báo lỗi thân thiện hơn (toast/dialog).

---

Mọi chức năng được thiết kế để giữ nguyên logic phía backend, do đó khi backend cập nhật API hoặc schema cần đồng bộ lại các model và dịch vụ tương ứng.

