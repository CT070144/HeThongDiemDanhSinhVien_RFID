# Hệ thống điểm danh sinh viên tự động sử dụng RFID và ESP32

## 📋 Tổng quan dự án

Hệ thống điểm danh sinh viên tự động sử dụng công nghệ RFID, ESP32 và web application hiện đại. Sinh viên chỉ cần quẹt thẻ RFID tại thiết bị ESP32, hệ thống sẽ tự động ghi nhận điểm danh, xác định ca học hiện tại, và hiển thị thông tin real-time trên website với đầy đủ tính năng quản lý lớp học phần, xuất báo cáo Excel và thống kê chi tiết.

### ✨ Tính năng nổi bật

- 🎯 **Điểm danh tự động**: Quẹt thẻ RFID để điểm danh vào/ra
- 📚 **Quản lý lớp học phần**: Tạo và quản lý các lớp học phần
- 📊 **Thống kê real-time**: Dashboard với biểu đồ và báo cáo chi tiết
- 📁 **Import/Export Excel**: Nhập danh sách sinh viên và xuất báo cáo
- 🔐 **Bảo mật**: Hệ thống đăng nhập với JWT authentication
- 📱 **Responsive**: Giao diện thân thiện trên mọi thiết bị
- 🌡️ **Cảm biến môi trường**: Hiển thị nhiệt độ, độ ẩm trên ESP32
- 📺 **OLED Display**: Hiển thị thông tin trực tiếp trên thiết bị

## 🏗️ Kiến trúc hệ thống

```
┌─────────────────┐    WiFi    ┌─────────────────┐    HTTP    ┌─────────────────┐
│   ESP32 + RFID  │ ────────── │  Spring Boot    │ ────────── │   ReactJS       │
│   + Sensors     │            │  Backend        │            │   Frontend      │
│   + OLED        │            │  + Security     │            │   + Charts      │
└─────────────────┘            └─────────────────┘            └─────────────────┘
                                        │
                                        │ JDBC
                                        ▼
                                 ┌─────────────────┐
                                 │   MySQL         │
                                 │   Database      │
                                 └─────────────────┘
```

### 🔧 Thành phần phần cứng

- **ESP32 Development Board**: Vi điều khiển chính
- **RC522 RFID Module**: Module đọc thẻ RFID
- **DHT11 Sensor**: Cảm biến nhiệt độ và độ ẩm
- **OLED Display 128x64**: Màn hình hiển thị thông tin
- **LED đa sắc**: Hiển thị các trạng thái khác nhau
- **Buzzer**: Còi báo âm thanh
- **Button**: Nút refresh thủ công

## 🛠️ Công nghệ sử dụng

### Backend Technologies
- **Spring Boot 3.2.0** - Main framework
- **Spring Data JPA** - Database ORM với lazy loading
- **Spring Security** - Authentication & Authorization
- **JWT (JSON Web Token)** - Token-based authentication
- **Apache POI** - Excel file processing
- **MySQL 8.0** - Database với timezone Asia/Ho_Chi_Minh
- **Maven** - Dependency management
- **WebSocket** - Real-time communication

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
- **DHT11** - Temperature/Humidity sensor
- **SSD1306 OLED** - 128x64 display module
- **Arduino IDE** - Development environment

### Development Tools
- **Java 17+** - Backend development
- **Node.js 16+** - Frontend development
- **Git** - Version control
- **Arduino IDE** - Embedded development

## 🚀 Cài đặt và chạy dự án

### 1. Yêu cầu hệ thống

- **Java 17+**
- **Node.js 16+**
- **MySQL 8.0+**
- **Arduino IDE** với ESP32 board package
- **Git**

### 2. Cài đặt Database

#### Option 1: Setup mới hoàn toàn (Khuyến nghị)
```bash
# Tạo database với dữ liệu mẫu đầy đủ
mysql -u root -p < ScriptDatabase/create_database_complete.sql
```

#### Option 2: Chỉ tạo cấu trúc bảng
```bash
# Tạo database chỉ có cấu trúc, không có dữ liệu mẫu
mysql -u root -p < ScriptDatabase/create_database_structure_only.sql
```

#### Option 3: Reset và tạo lại (⚠️ Xóa toàn bộ dữ liệu cũ)
```bash
# CẢNH BÁO: Xóa toàn bộ dữ liệu cũ
mysql -u root -p < ScriptDatabase/reset_and_create_database.sql
```

3. Cập nhật thông tin database trong `BackEnd/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/rfid_attendance_system?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
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

**Thông tin đăng nhập mặc định:**
- Username: `admin`
- Password: `admin123`
- Role: `ADMIN`

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

1. **Cài đặt Arduino IDE và ESP32 board package:**
   - Mở Arduino IDE
   - Vào File > Preferences
   - Thêm URL: `https://dl.espressif.com/dl/package_esp32_index.json`
   - Vào Tools > Board > Boards Manager
   - Tìm "ESP32" và cài đặt

2. **Cài đặt thư viện cần thiết:**
   - Vào Tools > Manage Libraries
   - Cài đặt: "MFRC522" by GithubCommunity
   - Cài đặt: "ArduinoJson" by Benoit Blanchon
   - Cài đặt: "DHT sensor library" by Adafruit
   - Cài đặt: "Adafruit SSD1306" by Adafruit
   - Cài đặt: "Adafruit GFX Library" by Adafruit

3. **Kết nối phần cứng:**
```
RC522 RFID Module:
VCC  ->  3.3V
GND  ->  GND
RST  ->  GPIO 22
SS   ->  GPIO 21
MOSI ->  GPIO 23
MISO ->  GPIO 19
SCK  ->  GPIO 18

DHT11 Sensor:
VCC  ->  3.3V
GND  ->  GND
DATA ->  GPIO 4

OLED Display (I2C):
VCC  ->  3.3V
GND  ->  GND
SDA  ->  GPIO 21
SCL  ->  GPIO 22

LEDs:
LED WiFi Success    -> GPIO 2
LED WiFi Fail       -> GPIO 15
LED Attendance Success -> GPIO 4
LED Attendance Fail -> GPIO 5
LED Card Detected   -> GPIO 18

Buzzer -> GPIO 19
Button -> GPIO 0 (INPUT_PULLUP)
```

4. **Cấu hình code:**
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

5. **Nạp code vào ESP32:**
   - Chọn board: ESP32 Dev Module
   - Chọn port COM tương ứng
   - Nhấn Upload

## 📱 Sử dụng hệ thống

### 1. 🔐 Đăng nhập hệ thống

- Truy cập: `http://localhost:3000/login`
- Sử dụng thông tin mặc định:
  - Username: `admin`
  - Password: `admin123`

### 2. 👥 Quản lý sinh viên

- Truy cập: `http://localhost:3000/students`
- **Tính năng:**
  - Thêm sinh viên mới với RFID, mã sinh viên, tên sinh viên
  - Chỉnh sửa, xóa thông tin sinh viên
  - Tìm kiếm sinh viên theo mã hoặc tên
  - Import danh sách sinh viên từ file Excel
  - Filter sinh viên theo lớp học phần

### 3. 📚 Quản lý lớp học phần

- Truy cập: `http://localhost:3000/courses`
- **Tính năng:**
  - Tạo và quản lý lớp học phần
  - Import danh sách sinh viên từ Excel cho từng lớp
  - Xem danh sách sinh viên trong lớp
  - Quản lý quan hệ sinh viên - lớp học phần

### 4. 🎯 Điểm danh tự động

- Sinh viên quẹt thẻ RFID tại thiết bị ESP32
- **Hệ thống tự động:**
  - Xác định ca học hiện tại dựa trên thời gian
  - Ghi nhận giờ vào/ra
  - Xác định trạng thái (đúng giờ/muộn)
  - Cập nhật real-time lên website
  - Hiển thị thông tin trên OLED display
  - Báo hiệu bằng LED và buzzer

### 5. 📊 Dashboard và thống kê

- Truy cập: `http://localhost:3000/dashboard`
- **Tính năng:**
  - Thống kê tổng quan real-time
  - Biểu đồ điểm danh theo ca học
  - Biểu đồ thống kê theo giờ
  - Biểu đồ trạng thái điểm danh
  - Thống kê tuần

### 6. 📋 Lịch sử điểm danh

- Truy cập: `http://localhost:3000/attendance`
- **Tính năng:**
  - Lọc theo ngày, ca học, mã sinh viên, lớp học phần
  - Xem thống kê chi tiết
  - Export dữ liệu ra Excel với định dạng đẹp
  - Pagination cho dữ liệu lớn

### 7. 🔍 Quản lý RFID chưa đăng ký

- Truy cập: `http://localhost:3000/rfid-reader`
- **Tính năng:**
  - Xem danh sách RFID chưa được đăng ký
  - Thêm sinh viên mới từ RFID đã đọc
  - Đánh dấu RFID đã xử lý
  - Bulk update nhiều RFID cùng lúc

## ⏰ Các ca học

Hệ thống hỗ trợ 4 ca học trong ngày:

- **Ca 1**: 07:00 - 09:30
- **Ca 2**: 09:30 - 12:00  
- **Ca 3**: 12:30 - 15:00
- **Ca 4**: 15:00 - 17:30

### 📊 Trạng thái điểm danh

- **DUNG_GIO**: Điểm danh đúng giờ
- **MUON**: Điểm danh muộn

### 🎓 Trạng thái học tập

- **DANG_HOC**: Đang học
- **DA_RA_VE**: Đã ra về
- **RA_VE_SOM**: Ra về sớm
- **KHONG_DIEM_DANH_RA**: Không điểm danh ra

## 🔌 API Endpoints

### 🔐 Authentication
- `POST /api/auth/login` - Đăng nhập
- `POST /api/auth/logout` - Đăng xuất
- `GET /api/auth/me` - Lấy thông tin user hiện tại

### 👥 Sinh viên
- `GET /api/sinhvien` - Lấy danh sách sinh viên (có pagination)
- `GET /api/sinhvien/{maSinhVien}` - Lấy thông tin sinh viên theo mã
- `POST /api/sinhvien` - Thêm sinh viên mới
- `PUT /api/sinhvien/{maSinhVien}` - Cập nhật sinh viên
- `DELETE /api/sinhvien/{maSinhVien}` - Xóa sinh viên
- `GET /api/sinhvien/search?keyword=` - Tìm kiếm sinh viên
- `GET /api/sinhvien/by-class/{maLopHocPhan}` - Lấy sinh viên theo lớp học phần
- `POST /api/sinhvien/import-excel` - Import sinh viên từ Excel

### 📚 Lớp học phần
- `GET /api/lophocphan` - Lấy danh sách lớp học phần
- `GET /api/lophocphan/{maLopHocPhan}` - Lấy thông tin lớp học phần
- `POST /api/lophocphan` - Tạo lớp học phần mới
- `PUT /api/lophocphan/{maLopHocPhan}` - Cập nhật lớp học phần
- `DELETE /api/lophocphan/{maLopHocPhan}` - Xóa lớp học phần
- `POST /api/lophocphan/import-students` - Import sinh viên cho lớp học phần
- `GET /api/lophocphan/{maLopHocPhan}/students` - Lấy danh sách sinh viên trong lớp

### 📋 Điểm danh
- `GET /api/attendance` - Lấy lịch sử điểm danh (có filter và pagination)
- `GET /api/attendance/today` - Điểm danh hôm nay
- `GET /api/attendance/statistics` - Thống kê điểm danh
- `POST /api/attendance/rfid` - Xử lý RFID từ ESP32
- `GET /api/attendance/filter` - Lọc điểm danh
- `POST /api/attendance/export-excel` - Export báo cáo Excel
- `PUT /api/attendance/{id}/status` - Cập nhật trạng thái điểm danh

### 🔍 RFID Management
- `GET /api/attendance/unprocessed-rfids` - RFID chưa xử lý
- `PUT /api/attendance/mark-processed/{id}` - Đánh dấu đã xử lý
- `PUT /api/attendance/bulk-process` - Bulk update nhiều RFID

### 🏢 Thiết bị
- `GET /api/thietbi` - Lấy danh sách thiết bị
- `POST /api/thietbi` - Thêm thiết bị mới
- `PUT /api/thietbi/{maThietBi}` - Cập nhật thiết bị
- `DELETE /api/thietbi/{maThietBi}` - Xóa thiết bị

## 🗄️ Cấu trúc Database

### 👥 Bảng users
- `id` (PK) - ID tự tăng
- `username` - Tên đăng nhập (unique)
- `password` - Mật khẩu (đã mã hóa)
- `full_name` - Họ và tên
- `email` - Email
- `role` - Vai trò (ADMIN/USER)
- `is_active` - Trạng thái hoạt động
- `created_at`, `updated_at`, `last_login` - Timestamps

### 👨‍🎓 Bảng sinhvien
- `masinhvien` (PK) - Mã sinh viên (khóa chính)
- `rfid` - Mã RFID duy nhất
- `tensinhvien` - Tên sinh viên
- `created_at`, `updated_at` - Timestamps

### 📚 Bảng lophocphan
- `malophocphan` (PK) - Mã lớp học phần
- `tenlophocphan` - Tên lớp học phần
- `created_at`, `updated_at` - Timestamps

### 🔗 Bảng sinhvienlophocphan
- `masinhvien` (FK) - Mã sinh viên
- `malophocphan` (FK) - Mã lớp học phần
- `created_at`, `updated_at` - Timestamps
- **Composite Primary Key**: (masinhvien, malophocphan)

### 📋 Bảng phieudiemdanh
- `id` (PK) - ID tự tăng
- `rfid` - Mã RFID
- `masinhvien`, `tensinhvien` - Thông tin sinh viên
- `phonghoc` - Phòng học
- `giovao`, `giora` - Giờ vào/ra
- `ngay` - Ngày điểm danh
- `ca` - Ca học (1-4)
- `tinhtrangdiemdanh` - Tình trạng điểm danh (DUNG_GIO/MUON)
- `trangthai` - Trạng thái học tập (DANG_HOC/DA_RA_VE/RA_VE_SOM/KHONG_DIEM_DANH_RA)
- `created_at`, `updated_at` - Timestamps

### 📖 Bảng docRfid
- `id` (PK) - ID tự tăng
- `rfid` - Mã RFID chưa đăng ký
- `masinhvien`, `tensinhvien` - Thông tin tùy chọn
- `processed` - Đã xử lý hay chưa (boolean)
- `created_at`, `updated_at` - Timestamps

### 🏢 Bảng thietbi
- `mathietbi` (PK) - Mã thiết bị
- `phonghoc` - Phòng học

## 🔧 Troubleshooting

### 🚫 ESP32 không kết nối WiFi
- Kiểm tra SSID và password trong code
- Đảm bảo ESP32 và máy tính cùng mạng
- Kiểm tra Serial Monitor để xem lỗi chi tiết
- Thử reset ESP32 bằng nút reset

### 🗄️ Backend không kết nối database
- Kiểm tra MySQL đang chạy
- Xác nhận thông tin kết nối trong application.properties
- Kiểm tra firewall và port 3306
- Kiểm tra timezone configuration

### 🌐 Frontend không gọi được API
- Kiểm tra backend đang chạy tại port 8080
- Xác nhận CORS configuration
- Kiểm tra network tab trong Developer Tools
- Kiểm tra JWT token có hợp lệ không

### 📡 RFID không đọc được thẻ
- Kiểm tra kết nối phần cứng RC522
- Đảm bảo thẻ RFID hoạt động
- Kiểm tra Serial Monitor để debug
- Kiểm tra nguồn điện 3.3V

### 📊 Lỗi Excel Import/Export
- Kiểm tra định dạng file Excel (.xls/.xlsx)
- Xác nhận cấu trúc cột trong file
- Kiểm tra quyền ghi file
- Kiểm tra Apache POI dependencies

### 🔐 Lỗi Authentication
- Kiểm tra JWT secret key
- Xác nhận token expiration time
- Kiểm tra Spring Security configuration
- Clear browser cache và cookies

## 🚀 Mở rộng hệ thống

### ✨ Tính năng đã có:
- ✅ **Authentication & Authorization** - Hệ thống đăng nhập JWT
- ✅ **Export báo cáo Excel** - Xuất báo cáo với định dạng đẹp
- ✅ **Import Excel** - Nhập dữ liệu từ file Excel
- ✅ **Real-time Dashboard** - Thống kê và biểu đồ real-time
- ✅ **Multi-class support** - Quản lý nhiều lớp học phần
- ✅ **Hardware sensors** - Nhiệt độ, độ ẩm, OLED display

### 🔮 Tính năng có thể thêm:
- 📧 **Email/SMS notifications** - Thông báo điểm danh
- 📱 **Mobile app** - Ứng dụng di động
- 👤 **Face recognition** - Nhận diện khuôn mặt
- 🔐 **Biometric integration** - Tích hợp sinh trắc học
- 🌍 **Multi-location support** - Hỗ trợ nhiều địa điểm
- 📊 **Advanced analytics** - Phân tích dữ liệu nâng cao
- 🔔 **Real-time notifications** - Thông báo real-time với WebSocket

### ⚡ Tối ưu hóa:
- 🗄️ **Caching với Redis** - Cache dữ liệu
- ⚖️ **Load balancing** - Cân bằng tải
- 📈 **Database indexing** - Tối ưu database
- 🐳 **Docker containerization** - Container hóa
- ☁️ **Cloud deployment** - Triển khai trên cloud

## 📚 Tài liệu tham khảo

### 📖 Documentation Files
- `PROJECT_COMPREHENSIVE_REPORT.md` - Báo cáo tổng hợp dự án
- `SYSTEM_FUNCTIONALITY_DESCRIPTION.md` - Mô tả chi tiết chức năng
- `FEATURE_GUIDE.md` - Hướng dẫn sử dụng tính năng
- `EXCEL_IMPORT_GUIDE.md` - Hướng dẫn import Excel
- `RFID_DEBUG_GUIDE.md` - Hướng dẫn debug RFID
- `TROUBLESHOOTING.md` - Khắc phục sự cố
- `ScriptDatabase/README_DATABASE_SCRIPTS.md` - Hướng dẫn database scripts

### 🔧 Hardware Documentation
- `hardware_circuit_diagram.md` - Sơ đồ mạch điện
- `HARDWARE_CONNECTIONS.md` - Hướng dẫn kết nối phần cứng

## 🆘 Liên hệ và hỗ trợ

Nếu gặp vấn đề trong quá trình cài đặt hoặc sử dụng, vui lòng:

1. 📋 Kiểm tra log files trong console
2. 📺 Xem Serial Monitor của ESP32
3. 🔍 Kiểm tra Developer Tools của browser
4. 📚 Tham khảo documentation files trong project
5. 🐛 Kiểm tra `TROUBLESHOOTING.md` để xem giải pháp

## 📄 License

Dự án này được phát triển cho mục đích học tập và nghiên cứu.

---

**🎯 Lưu ý**: Đây là phiên bản hoàn chỉnh của hệ thống điểm danh RFID với đầy đủ tính năng quản lý lớp học phần, authentication, import/export Excel, và thống kê real-time. Hệ thống đã được tối ưu hóa và sẵn sàng cho môi trường production.
