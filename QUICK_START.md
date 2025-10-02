# Hướng dẫn cài đặt nhanh

## Bước 1: Chuẩn bị môi trường

### Cài đặt phần mềm cần thiết:
1. **Java 17+** - [Download Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
2. **Node.js 16+** - [Download Node.js](https://nodejs.org/)
3. **MySQL 8.0+** - [Download MySQL](https://dev.mysql.com/downloads/)
4. **Arduino IDE** - [Download Arduino IDE](https://www.arduino.cc/en/software)

### Cài đặt ESP32 board package cho Arduino IDE:
1. Mở Arduino IDE
2. File > Preferences
3. Thêm URL: `https://dl.espressif.com/dl/package_esp32_index.json`
4. Tools > Board > Boards Manager
5. Tìm "ESP32" và cài đặt

## Bước 2: Cài đặt Database

```sql
-- Tạo database
CREATE DATABASE rfid_attendance_system;

-- Chạy script tạo bảng
SOURCE ScriptDatabase/create_database.sql;
```

## Bước 3: Cấu hình Backend

1. Vào thư mục `BackEnd`
2. Cập nhật `src/main/resources/application.properties`:
```properties
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

3. Chạy backend:
```bash
mvn spring-boot:run
```

## Bước 4: Cấu hình Frontend

1. Vào thư mục `FrontEnd`
2. Cài đặt dependencies:
```bash
npm install
```

3. Chạy frontend:
```bash
npm start
```

## Bước 5: Cấu hình ESP32

1. Cài đặt thư viện trong Arduino IDE:
   - Tools > Manage Libraries > Tìm "MFRC522"
   - Tools > Manage Libraries > Tìm "ArduinoJson"

2. Mở file `File .ino/RFID_Attendance_ESP32.ino`

3. Cập nhật thông tin:
```cpp
const char* ssid = "YOUR_WIFI_NAME";
const char* password = "YOUR_WIFI_PASSWORD";
const char* serverURL = "http://YOUR_COMPUTER_IP:8080/api/attendance/rfid";
```

4. Kết nối phần cứng theo sơ đồ trong README

5. Upload code vào ESP32

## Bước 6: Kiểm tra hệ thống

1. Truy cập `http://localhost:3000`
2. Thêm sinh viên mới
3. Test quẹt thẻ RFID
4. Kiểm tra lịch sử điểm danh

## Troubleshooting nhanh

- **ESP32 không kết nối WiFi**: Kiểm tra SSID/password
- **Backend lỗi database**: Kiểm tra MySQL đang chạy
- **Frontend không load**: Kiểm tra backend đang chạy port 8080
- **RFID không đọc**: Kiểm tra kết nối phần cứng
