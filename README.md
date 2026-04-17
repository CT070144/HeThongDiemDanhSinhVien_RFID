# Hệ thống chấm công RFID + FaceRecognition (ESP32 + Web)

## 📋 Tổng quan

Hệ thống chấm công tự động sử dụng **RFID (RC522) + ESP32** kết hợp **Web Admin (ReactJS)** và **Backend (Spring Boot + MySQL)**. Khi nhân viên quẹt thẻ, hệ thống ghi nhận **giờ vào/giờ ra**, xác định **ca làm**, cập nhật **realtime** lên giao diện. Với nhân viên đã đăng ký khuôn mặt, hệ thống có thể yêu cầu **xác thực khuôn mặt** trước khi chấm công.

Hệ thống gồm 4 phần chính:

- `BackEnd`: Spring Boot xử lý API, bảo mật, chấm công, realtime Socket.IO, lưu trữ ảnh chấm công.
- `FrontEnd`: ReactJS giao diện quản trị, dashboard, lịch sử chấm công.
- `FaceRecognition`: Python Flask dùng để **encode** và **compare** khuôn mặt.
- `Embedded`: ESP32 đọc thẻ RFID, gửi dữ liệu lên Backend, hiển thị trạng thái trên LCD.

### ✨ Tính năng chính

- 🎯 **Chấm công vào/ra** theo RFID, tự xác định ca làm theo thời gian
- 🔐 **Bảo mật**: JWT cho người dùng web; **API Key** cho thiết bị ESP32
- 🧑‍💻 **Quản lý nhân viên** (RFID, thông tin cơ bản) + **đăng ký faceid** (vector khuôn mặt)
- 🏢 **Quản lý thiết bị** (mã thiết bị, vị trí/phòng)
- 📡 **Realtime** thông báo qua Socket.IO (cập nhật phiếu chấm công, cảnh báo thẻ lạ, yêu cầu chụp khuôn mặt)
- 📁 **Export Excel** báo cáo chấm công (tổng hợp + chi tiết)

## 🏗️ Kiến trúc hệ thống

```
┌─────────────────┐    WiFi    ┌─────────────────┐    HTTP    ┌─────────────────┐
│   ESP32 + RFID  │ ────────── │  Spring Boot    │ ────────── │   ReactJS       │
│   + LCD1602     │            │  Backend        │            │   Frontend      │
│   + API Key     │            │  + Security     │            │   + Dashboard   │
└─────────────────┘            └─────────────────┘            └─────────────────┘
                                        │
                                        │ JDBC
                                        ▼
                                 ┌─────────────────┐
                                 │   MySQL         │
                                 │   Database      │
                                 └─────────────────┘

   (Tuỳ chọn) FaceRecognition (Python) ↔ Backend
```

### 🔧 Thành phần phần cứng

- **ESP32 Development Board**: Vi điều khiển chính
- **RC522 RFID Module**: Module đọc thẻ RFID
- **LCD1602 I2C**: Màn hình hiển thị trạng thái
- **Buzzer**: Còi báo âm thanh
- **Button**: Nút cấu hình/reset (theo chương trình ESP32)

## 🛠️ Công nghệ sử dụng

### Backend Technologies
- **Spring Boot 3.2.0** - Main framework
- **Spring Data JPA** - Database ORM với lazy loading
- **Spring Security** - Authentication & Authorization
- **JWT (JSON Web Token)** - Token-based authentication
- **Apache POI** - Excel file processing
- **MySQL 8.0** - Database với timezone Asia/Ho_Chi_Minh
- **Maven** - Dependency management
- **Socket.IO (netty-socketio)** - Realtime communication

### Frontend Technologies
- **React.js 18** - UI framework với hooks
- **React Router 6** - Client-side routing
- **Bootstrap 5** - CSS framework
- **Chart.js & react-chartjs-2** - Data visualization
- **Axios** - HTTP client với interceptors
- **React Toastify** - Notifications
- **xlsx** - Excel file handling
- **React DatePicker** - Date selection

### Hardware Technologies
- **ESP32** - Microcontroller với WiFi
- **RC522** - RFID reader module
- **LCD1602 I2C** - 16x2 display
- **Arduino IDE** - Development environment

### Development Tools
- **Java 17+** - Backend development
- **Node.js 16+** - Frontend development
- **Git** - Version control
- **Arduino IDE** - Embedded development

## 🚀 Cài đặt và chạy dự án

### 1) Yêu cầu hệ thống

- **Java 17+**
- **Node.js 16+**
- **MySQL 8.0+** (hoặc dùng Docker Compose)
- **Python 3.10+** (nếu dùng FaceRecognition)
- **Arduino IDE** với ESP32 board package
- **Git**

### 2) Chạy nhanh bằng Docker Compose (khuyến nghị)

Mở terminal tại thư mục dự án và chạy:

```bash
docker compose up --build
```

Các service trong `docker-compose.yml`:

- `mysql`: `3307:3306`
- `backend`: `8080:8080` và socket `8099:8099`
- `frontend`: `3000:3000`

Truy cập:

- FrontEnd: `http://localhost:3000`
- BackEnd API: `http://localhost:8080`
- Socket realtime: `http://localhost:8099`
- MySQL host port: `localhost:3307`

Database được nạp từ script:

- `ScriptDatabase/rfid_attendance_system_backup.sql`

> Lưu ý: Docker Compose hiện **chưa** chạy `FaceRecognition` (Python). Nếu cần xác thực khuôn mặt, hãy chạy theo mục “FaceRecognition”.

### 3) Chạy thủ công Backend

- Import DB (nếu không dùng Docker Compose):

```bash
mysql -u root -p < ScriptDatabase/rfid_attendance_system_backup.sql
```

- Chạy Backend:

```bash
cd BackEnd
./mvnw spring-boot:run
```

### 4) Chạy thủ công FrontEnd

```bash
cd FrontEnd
npm install
npm start
```

Biến môi trường (tuỳ chọn) trong `FrontEnd/.env`:

```env
REACT_APP_API_URL=http://localhost:8080/api
REACT_APP_SOCKET_URL=http://localhost:8099
```

### 5) FaceRecognition (Python, tuỳ chọn)

File: `FaceRecognition/face_recognition.py`

- Tạo môi trường & cài thư viện:

```bash
cd FaceRecognition
python -m venv .venv
# Windows:
.\.venv\Scripts\activate
pip install flask face-recognition numpy opencv-python
```

- Chạy service:

```bash
python face_recognition.py
```

Service chạy tại `http://localhost:5000` với 2 endpoint:

- `POST /encode` (multipart field `files`): trả vector `encoding` 128 chiều
- `POST /compare` (multipart field `file`, form field `encoding`): trả `match/distance`

### 6) ESP32 (Embedded)

File chương trình: `Embedded/FaceRecognize+RFID.ino`

Thư viện Arduino cần cài:

- `MFRC522`
- `ArduinoJson`
- `LiquidCrystal_I2C`
- `WiFiManager`

Kết nối phần cứng theo code:

```
RC522 RFID Module:
VCC  ->  VIN hoặc 3.3V
GND  ->  GND
RST  ->  GPIO 4
SS   ->  GPIO 5
MOSI ->  GPIO 23
MISO ->  GPIO 19
SCK  ->  GPIO 18

LCD1602 I2C:
VCC  ->  3.3V hoặc 5V
GND  ->  GND
SDA  ->  GPIO 21
SCL  ->  GPIO 22

Buzzer -> GPIO 15
Button -> GPIO 0 (INPUT_PULLUP)
```

Thiết bị hỗ trợ cấu hình WiFi + `Device ID` + `Server URL` + `API Key` qua WiFiManager:

- AP: `RFID-Device`
- Mật khẩu: `12345678`

Quan trọng:

- `serverURL` phải trỏ đúng IP LAN của máy chạy Backend (không dùng `localhost`)
- `API Key` phải khớp với API key đang active của thiết bị (Backend)

## 📱 Sử dụng hệ thống

### Luồng cơ bản

1. Chạy MySQL + Backend + FrontEnd (và FaceRecognition nếu dùng).
2. Đăng nhập web admin tại `http://localhost:3000`.
3. Tạo/cập nhật **thiết bị** (mã thiết bị, vị trí/phòng).
4. Tạo/cập nhật **nhân viên** và gán RFID.
5. (Tuỳ chọn) Upload ảnh để tạo `faceid`.
6. Nhân viên quẹt thẻ RFID trên ESP32 → hệ thống ghi nhận chấm công và cập nhật realtime.

### Realtime & xác thực khuôn mặt

- Khi Backend yêu cầu xác thực khuôn mặt, FrontEnd sẽ nhận event `request-face-capture` và chụp 1 frame từ camera để gửi lên Backend.
- Nếu trình duyệt không có quyền camera, hệ thống sẽ hiển thị thông báo lỗi quyền camera.

## 🔌 API (tóm tắt)

- Auth: `POST /api/auth/login`
- Chấm công: `POST /api/attendance/rfid` (JSON từ ESP32 hoặc multipart khi kèm ảnh)
- Lịch sử (phân trang): `GET /api/attendance/paged`
- Thiết bị: `/api/thietbi/*`
- Nhân viên: `/api/sinhvien/*`

## 🔧 Troubleshooting

### 🚫 ESP32 không kết nối WiFi
- Kiểm tra ESP32 và máy chủ Backend cùng mạng LAN/WiFi
- Kiểm tra cấu hình `serverURL` trên thiết bị
- Xem log trong Serial Monitor (baud 115200)

### 🗄️ Backend không kết nối database
- Kiểm tra MySQL đang chạy
- Nếu dùng Docker Compose: MySQL host port là `3307`

### 🌐 Frontend không gọi được API
- Kiểm tra backend đang chạy tại port 8080
- Kiểm tra JWT token có hợp lệ không
- Kiểm tra `REACT_APP_API_URL`, `REACT_APP_SOCKET_URL`

### 📡 RFID không đọc được thẻ
- Kiểm tra kết nối phần cứng RC522
- Đảm bảo thẻ RFID hoạt động
- Kiểm tra nguồn cấp (VIN/3.3V), dây SPI và chân SS/RST đúng theo code

## 📄 License

Dự án này được phát triển cho mục đích học tập và nghiên cứu.
