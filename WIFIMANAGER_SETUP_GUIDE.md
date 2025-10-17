# Hướng dẫn cài đặt WiFiManager cho ESP32 RFID System

## 📋 Tổng quan

WiFiManager cho phép người dùng cấu hình WiFi, password và device_id thông qua web portal mà không cần chỉnh sửa code. Thiết bị sẽ tự động tạo một Access Point để người dùng kết nối và cấu hình.

## 🔧 Cài đặt thư viện

### Bước 1: Cài đặt WiFiManager
1. Mở Arduino IDE
2. Vào **Tools** → **Manage Libraries**
3. Tìm kiếm "**WiFiManager**"
4. Cài đặt thư viện "**WiFiManager by tzapu**"

### Bước 2: Cài đặt Preferences (nếu chưa có)
1. Tìm kiếm "**Preferences**"
2. Cài đặt thư viện "**Preferences by Christopher Baker**"

## 📱 Cách sử dụng

### Lần đầu sử dụng (Chưa có WiFi credentials)
1. **Upload code** lên ESP32
2. **Khởi động** thiết bị
3. ESP32 sẽ tự động tạo Access Point tên "**RFID-Device**"
4. Mật khẩu: "**12345678**"
5. Kết nối điện thoại/laptop vào WiFi "RFID-Device"
6. Mở trình duyệt web, sẽ tự động hiện trang cấu hình
7. Nếu không tự động, truy cập: **http://192.168.4.1**

### Trang cấu hình
Trong trang web, bạn có thể cấu hình:

#### 1. **WiFi Settings**
- **WiFi SSID**: Tên mạng WiFi muốn kết nối
- **WiFi Password**: Mật khẩu WiFi

#### 2. **Device Settings**
- **Device ID**: Mã thiết bị (mặc định: DEVICE_001)
- **Server URL**: URL server Spring Boot (mặc định: http://192.168.1.70:8080/api/attendance/rfid)

#### 3. **Save Configuration**
- Nhấn nút "**Save**" để lưu cấu hình
- Thiết bị sẽ tự động khởi động lại và kết nối WiFi

### Lần sử dụng tiếp theo
- Thiết bị sẽ tự động kết nối với WiFi đã lưu
- Không cần cấu hình lại

## 🔄 Reset cấu hình

### Cách 1: Reset khi khởi động
1. **Giữ nút** trên ESP32 khi bật nguồn
2. Đợi 3 giây, nếu vẫn giữ nút → **Reset WiFi credentials**
3. Thiết bị sẽ vào chế độ cấu hình lại

### Cách 2: Reset khi đang hoạt động
1. **Nhấn nút** một lần → Hiển thị "Hold 5s to Reset Config"
2. **Giữ nút** trong 5 giây → Reset toàn bộ cấu hình
3. Thiết bị sẽ khởi động lại và vào chế độ cấu hình

### Cách 3: Reset bằng Serial Monitor
```cpp
// Gửi lệnh qua Serial Monitor
// Thiết bị sẽ clear tất cả cấu hình và restart
```

## 📊 Trạng thái hiển thị trên LCD

### Khi khởi động:
- "**WiFi Setup**"
- "**Starting...**"

### Khi cấu hình:
- "**Starting Config**"
- "**Portal...**"

### Khi kết nối thành công:
- "**WiFi Connected!**"
- Hiển thị IP address

### Khi mất kết nối:
- "**WiFi Disconnected**"
- "**Reconnecting...**"

### Khi reset cấu hình:
- "**Hold 5s to**"
- "**Reset Config**"
- "**Resetting**"
- "**Config...**"
- "**Config Reset!**"
- "**Restarting...**"

## 🔧 Cấu hình nâng cao

### Thay đổi timeout cấu hình
```cpp
// Trong setupWiFiManager()
wm.setConfigPortalTimeout(180); // 3 phút (180 giây)
```

### Thay đổi hostname
```cpp
// Trong setupWiFiManager()
wm.setHostname("RFID-Device"); // Tên Access Point
```

### Thay đổi mật khẩu Access Point
```cpp
// Trong setupWiFiManager()
wm.startConfigPortal("RFID-Device", "12345678");
//                                 ↑        ↑
//                            hostname   password
```

### Thêm tham số tùy chỉnh
```cpp
// Thêm tham số mới
WiFiManagerParameter custom_param("param_name", "Label", "default_value", 20);
wm.addParameter(&custom_param);
```

## 🚨 Xử lý sự cố

### Thiết bị không tạo Access Point
1. Kiểm tra kết nối WiFi hiện tại
2. Reset thiết bị
3. Kiểm tra Serial Monitor để xem lỗi

### Không thể truy cập trang cấu hình
1. Kiểm tra đã kết nối đúng WiFi "RFID-Device"
2. Thử truy cập http://192.168.4.1 thủ công
3. Kiểm tra firewall/antivirus

### Cấu hình không được lưu
1. Kiểm tra quyền ghi flash của ESP32
2. Thử reset và cấu hình lại
3. Kiểm tra Serial Monitor

### Thiết bị không kết nối WiFi sau cấu hình
1. Kiểm tra thông tin WiFi đúng chưa
2. Kiểm tra tín hiệu WiFi
3. Thử reset cấu hình và nhập lại

## 📝 Lưu ý quan trọng

### Bảo mật
- **Không** sử dụng mật khẩu Access Point mặc định trong production
- **Thay đổi** hostname để tránh xung đột
- **Kiểm tra** server URL trước khi lưu

### Hiệu suất
- Timeout cấu hình: **3 phút** (có thể điều chỉnh)
- Thời gian reconnect: **5 giây**
- Thời gian chờ kết nối: **10 lần thử**

### Tương thích
- **ESP32** (không tương thích với ESP8266)
- **Arduino IDE** 1.8.0 trở lên
- **WiFiManager** version 2.0.0 trở lên

## 🔍 Debug và Monitoring

### Serial Monitor Output
```
RFID Attendance System Starting...
Configuration loaded from flash:
SSID: YourWiFiName
Device ID: DEVICE_001
Server URL: http://192.168.1.70:8080/api/attendance/rfid
WiFi Setup Starting...
Trying to connect with saved credentials...
WiFi connected with saved credentials!
IP address: 192.168.1.100
```

### Kiểm tra trạng thái WiFi
```cpp
// Gọi hàm này để in thông tin WiFi
printWiFiStatus();
```

### Kiểm tra cấu hình đã lưu
```cpp
// Kiểm tra trong Serial Monitor
Serial.println("Device ID: " + DEVICE_ID);
Serial.println("Server URL: " + String(serverURL));
```

## 📞 Hỗ trợ

Nếu gặp vấn đề:
1. Kiểm tra Serial Monitor output
2. Xem log trên LCD
3. Thử reset cấu hình
4. Kiểm tra kết nối phần cứng
5. Đảm bảo thư viện đã cài đặt đúng

---

**Phiên bản**: 1.0  
**Cập nhật**: 2024  
**Tương thích**: ESP32 + WiFiManager 2.0+
