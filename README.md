# Hệ thống điểm danh sinh viên sử dụng RFID và ESP32

## Tổng quan dự án

Hệ thống điểm danh sinh viên tự động sử dụng thẻ RFID, ESP32 và web application. Sinh viên chỉ cần quẹt thẻ RFID tại thiết bị ESP32, hệ thống sẽ tự động ghi nhận điểm danh và hiển thị thông tin real-time trên website.

## Kiến trúc hệ thống

```
┌─────────────────┐    WiFi    ┌─────────────────┐    HTTP    ┌─────────────────┐
│   ESP32 + RFID  │ ────────── │  Spring Boot    │ ────────── │   ReactJS       │
│   Module        │            │  Backend        │            │   Frontend      │
└─────────────────┘            └─────────────────┘            └─────────────────┘
                                        │
                                        │ JDBC
                                        ▼
                                 ┌─────────────────┐
                                 │   MySQL         │
                                 │   Database      │
                                 └─────────────────┘
```

## Công nghệ sử dụng

### Backend
- **Spring Boot 3.2.0** - Framework Java
- **Spring Data JPA** - ORM cho database
- **MySQL 8.0** - Database
- **Spring Security** - Bảo mật
- **JWT** - Authentication

### Frontend
- **ReactJS 18** - UI Framework
- **React Router** - Routing
- **Bootstrap 5** - CSS Framework
- **Axios** - HTTP Client
- **React Toastify** - Notifications

### Hardware
- **ESP32** - Microcontroller
- **RC522** - RFID Module
- **LEDs** - Status indicators

## Cài đặt và chạy dự án

### 1. Yêu cầu hệ thống

- **Java 17+**
- **Node.js 16+**
- **MySQL 8.0+**
- **Arduino IDE** với ESP32 board package
- **Git**

### 2. Cài đặt Database

1. Tạo database MySQL:
```sql
CREATE DATABASE rfid_attendance_system;
```

2. Chạy script tạo bảng:
```bash
mysql -u root -p rfid_attendance_system < ScriptDatabase/create_database.sql
```

3. Cập nhật thông tin database trong `BackEnd/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/rfid_attendance_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

### 3. Cài đặt và chạy Backend (Spring Boot)

1. Vào thư mục Backend:
```bash
cd BackEnd
```

2. Cài đặt dependencies:
```bash
mvn clean install
```

3. Chạy ứng dụng:
```bash
mvn spring-boot:run
```

Backend sẽ chạy tại: `http://localhost:8080`

### 4. Cài đặt và chạy Frontend (ReactJS)

1. Vào thư mục Frontend:
```bash
cd FrontEnd
```

2. Cài đặt dependencies:
```bash
npm install
```

3. Chạy ứng dụng:
```bash
npm start
```

Frontend sẽ chạy tại: `http://localhost:3000`

### 5. Cài đặt và nạp code cho ESP32

1. Cài đặt Arduino IDE và ESP32 board package:
   - Mở Arduino IDE
   - Vào File > Preferences
   - Thêm URL: `https://dl.espressif.com/dl/package_esp32_index.json`
   - Vào Tools > Board > Boards Manager
   - Tìm "ESP32" và cài đặt

2. Cài đặt thư viện cần thiết:
   - Vào Tools > Manage Libraries
   - Cài đặt: "MFRC522" by GithubCommunity
   - Cài đặt: "ArduinoJson" by Benoit Blanchon

3. Kết nối phần cứng:
```
RC522    ESP32
VCC  ->  3.3V
GND  ->  GND
RST  ->  GPIO 22
SS   ->  GPIO 21
MOSI ->  GPIO 23
MISO ->  GPIO 19
SCK  ->  GPIO 18
```

4. Cấu hình code:
   - Mở file `File .ino/RFID_Attendance_ESP32.ino`
   - Cập nhật thông tin WiFi:
   ```cpp
   const char* ssid = "YOUR_WIFI_SSID";
   const char* password = "YOUR_WIFI_PASSWORD";
   ```
   - Cập nhật địa chỉ server:
   ```cpp
   const char* serverURL = "http://YOUR_COMPUTER_IP:8080/api/attendance/rfid";
   ```

5. Nạp code vào ESP32:
   - Chọn board: ESP32 Dev Module
   - Chọn port COM tương ứng
   - Nhấn Upload

## Sử dụng hệ thống

### 1. Quản lý sinh viên

- Truy cập: `http://localhost:3000/students`
- Thêm sinh viên mới với RFID, mã sinh viên, tên sinh viên
- Chỉnh sửa, xóa thông tin sinh viên
- Tìm kiếm sinh viên theo mã hoặc tên

### 2. Điểm danh tự động

- Sinh viên quẹt thẻ RFID tại thiết bị ESP32
- Hệ thống tự động:
  - Xác định ca học hiện tại
  - Ghi nhận giờ vào/ra
  - Xác định trạng thái (đúng giờ/muộn)
  - Cập nhật real-time lên website

### 3. Xem lịch sử điểm danh

- Truy cập: `http://localhost:3000/attendance`
- Lọc theo ngày, ca học, mã sinh viên
- Xem thống kê tổng quan
- Export dữ liệu (tùy chọn)

### 4. Quản lý RFID chưa đăng ký

- Truy cập: `http://localhost:3000/rfid-reader`
- Xem danh sách RFID chưa được đăng ký
- Thêm sinh viên mới từ RFID đã đọc
- Đánh dấu RFID đã xử lý

## Các ca học

Hệ thống hỗ trợ 4 ca học trong ngày:

- **Ca 1**: 07:00 - 09:30
- **Ca 2**: 09:30 - 12:00  
- **Ca 3**: 12:30 - 15:00
- **Ca 4**: 15:00 - 17:30

## API Endpoints

### Sinh viên
- `GET /api/sinhvien` - Lấy danh sách sinh viên
- `POST /api/sinhvien` - Thêm sinh viên mới
- `PUT /api/sinhvien/{rfid}` - Cập nhật sinh viên
- `DELETE /api/sinhvien/{rfid}` - Xóa sinh viên
- `GET /api/sinhvien/search?keyword=` - Tìm kiếm sinh viên

### Điểm danh
- `GET /api/attendance` - Lấy lịch sử điểm danh
- `GET /api/attendance/today` - Điểm danh hôm nay
- `POST /api/attendance/rfid` - Xử lý RFID từ ESP32
- `GET /api/attendance/filter` - Lọc điểm danh

### RFID
- `GET /api/attendance/unprocessed-rfids` - RFID chưa xử lý
- `PUT /api/attendance/mark-processed/{id}` - Đánh dấu đã xử lý

## Cấu trúc Database

### Bảng sinhvien
- `rfid` (PK) - Mã RFID duy nhất
- `masinhvien` - Mã sinh viên
- `tensinhvien` - Tên sinh viên
- `created_at`, `updated_at` - Timestamps

### Bảng phieudiemdanh
- `id` (PK) - ID tự tăng
- `rfid` (FK) - Liên kết với sinhvien
- `masinhvien`, `tensinhvien` - Thông tin sinh viên
- `giovao`, `giora` - Giờ vào/ra
- `ngay` - Ngày điểm danh
- `ca` - Ca học (1-4)
- `trangthai` - Trạng thái (muon/dang_hoc/da_ra_ve)

### Bảng docRfid
- `id` (PK) - ID tự tăng
- `rfid` - Mã RFID chưa đăng ký
- `masinhvien`, `tensinhvien` - Thông tin tùy chọn
- `processed` - Đã xử lý hay chưa

## Troubleshooting

### ESP32 không kết nối WiFi
- Kiểm tra SSID và password
- Đảm bảo ESP32 và máy tính cùng mạng
- Kiểm tra Serial Monitor để xem lỗi chi tiết

### Backend không kết nối database
- Kiểm tra MySQL đang chạy
- Xác nhận thông tin kết nối trong application.properties
- Kiểm tra firewall và port 3306

### Frontend không gọi được API
- Kiểm tra backend đang chạy tại port 8080
- Xác nhận CORS configuration
- Kiểm tra network tab trong Developer Tools

### RFID không đọc được thẻ
- Kiểm tra kết nối phần cứng
- Đảm bảo thẻ RFID hoạt động
- Kiểm tra Serial Monitor để debug

## Mở rộng hệ thống

### Tính năng có thể thêm:
- Authentication và authorization
- Export báo cáo Excel/PDF
- Thông báo email/SMS
- Mobile app
- Face recognition
- Biometric integration
- Multi-location support

### Tối ưu hóa:
- Caching với Redis
- Load balancing
- Database indexing
- Real-time notifications với WebSocket
- Docker containerization

## Liên hệ và hỗ trợ

Nếu gặp vấn đề trong quá trình cài đặt hoặc sử dụng, vui lòng:

1. Kiểm tra log files
2. Xem Serial Monitor của ESP32
3. Kiểm tra Developer Tools của browser
4. Tham khảo documentation của các thư viện

## License

Dự án này được phát triển cho mục đích học tập và nghiên cứu.

---

**Lưu ý**: Đây là phiên bản demo của hệ thống điểm danh RFID. Trong môi trường production, cần thêm các tính năng bảo mật và tối ưu hóa phù hợp.
