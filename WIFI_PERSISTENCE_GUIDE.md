# Hướng dẫn sử dụng tính năng lưu cấu hình WiFi

## 🎯 **Tính năng mới đã được cải thiện**

ESP32 giờ đây sẽ **tự động lưu và khôi phục** thông tin WiFi sau khi mất nguồn!

## 🔧 **Cách hoạt động**

### **1. Lần đầu cấu hình WiFi:**
- Khi ESP32 khởi động lần đầu (không có cấu hình WiFi)
- Tự động tạo Access Point "RFID-Device" với password "12345678"
- Truy cập `http://192.168.4.1` để cấu hình

### **2. Cấu hình WiFi:**
1. **Kết nối WiFi:** Chọn SSID và nhập password
2. **Cấu hình Device ID:** Nhập mã thiết bị (VD: DEVICE_001)
3. **Cấu hình Server URL:** Nhập địa chỉ server (VD: http://192.168.1.70:8080/api/attendance/rfid)
4. **Lưu cấu hình:** Click "Save"

### **3. Lưu trữ tự động:**
- ✅ **SSID và Password** được lưu vào flash memory
- ✅ **Device ID** được lưu vào flash memory  
- ✅ **Server URL** được lưu vào flash memory
- ✅ **Flag xác nhận** cấu hình đã được lưu

### **4. Khôi phục sau khi mất nguồn:**
- ESP32 tự động đọc cấu hình từ flash memory
- Thử kết nối với WiFi đã lưu
- Nếu thành công → Sẵn sàng sử dụng
- Nếu thất bại → Mở lại config portal

## 📋 **Quy trình khởi động**

```
ESP32 Khởi động
      ↓
Đọc cấu hình từ Flash
      ↓
Có cấu hình WiFi? → Có → Thử kết nối
      ↓                    ↓
     Không              Thành công?
      ↓                    ↓
Mở Config Portal         Có → Sẵn sàng
      ↓                    ↓
Người dùng cấu hình       Không → Mở Config Portal
      ↓
Lưu cấu hình vào Flash
      ↓
Kết nối WiFi thành công
```

## 🔍 **Debug và kiểm tra**

### **Serial Monitor sẽ hiển thị:**

#### **Khi khởi động:**
```
Configuration loaded from flash:
SSID: MyWiFi
Password: [SAVED]
Device ID: DEVICE_001
Server URL: http://192.168.1.70:8080/api/attendance/rfid
WiFi Saved Flag: YES

=== WiFi Configuration Status ===
Saved SSID: MyWiFi
Saved Password: [SAVED]
Current SSID: MyWiFi
WiFi Status: 3
Connection Status: CONNECTED
✓ Saved credentials match current connection
=====================================
```

#### **Khi kết nối thành công:**
```
Trying to connect with saved credentials...
SSID: MyWiFi
Password: [SAVED]
Connecting to MyWiFi
.............
WiFi connected with saved credentials!
IP address: 192.168.1.100
Signal strength: -45 dBm
```

#### **Khi lưu cấu hình:**
```
=== Configuration Saved Successfully ===
WiFi SSID: MyWiFi
WiFi Password: [SAVED]
Device ID: DEVICE_001
Server URL: http://192.168.1.70:8080/api/attendance/rfid
=========================================
```

## 🚨 **Xử lý sự cố**

### **1. ESP32 không kết nối được WiFi đã lưu:**
- **Nguyên nhân:** WiFi đã đổi password hoặc không còn hoạt động
- **Giải pháp:** 
  - Giữ nút BOOT 5 giây để reset cấu hình
  - Hoặc đợi 3 phút để config portal tự động mở

### **2. Mất cấu hình sau khi mất nguồn:**
- **Nguyên nhân:** Flash memory bị lỗi hoặc code không lưu đúng
- **Giải pháp:** 
  - Kiểm tra Serial Monitor để xem có thông báo lỗi
  - Reset lại cấu hình bằng nút BOOT

### **3. Không thể truy cập config portal:**
- **Nguyên nhân:** ESP32 không tạo được Access Point
- **Giải pháp:**
  - Kiểm tra nguồn điện
  - Reset ESP32
  - Kiểm tra Serial Monitor

## 🔄 **Kết nối lại tự động**

### **Khi mất kết nối WiFi:**
```
WiFi disconnected. Attempting to reconnect...
Reconnecting to saved WiFi: MyWiFi
..........
WiFi reconnected successfully!
IP address: 192.168.1.100
```

### **Khi không thể kết nối lại:**
```
Failed to reconnect to WiFi
Reconnect Failed
Check WiFi
```

## ⚙️ **Cấu hình nâng cao**

### **Thay đổi thông số:**
- **Timeout kết nối:** 15 giây (có thể điều chỉnh)
- **Số lần thử lại:** 10 lần (có thể điều chỉnh)
- **Config portal timeout:** 3 phút (có thể điều chỉnh)

### **Tùy chỉnh trong code:**
```cpp
const int maxAttempts = 15; // Số lần thử kết nối
wm.setConfigPortalTimeout(180); // 3 phút timeout
```

## 📱 **LCD Display**

### **Trạng thái hiển thị:**
- **"WiFi Setup"** → Đang khởi tạo
- **"Connecting to [SSID]"** → Đang kết nối
- **"WiFi Connected!"** → Kết nối thành công
- **"WiFi Disconnected"** → Mất kết nối
- **"Reconnecting..."** → Đang kết nối lại
- **"Config Mode"** → Đang mở config portal

## ✅ **Lợi ích**

1. **Tự động kết nối:** Không cần cấu hình lại sau khi mất nguồn
2. **Tiết kiệm thời gian:** Không cần mở config portal mỗi lần
3. **Ổn định:** Tự động kết nối lại khi mất WiFi
4. **Debug dễ dàng:** Serial Monitor hiển thị chi tiết trạng thái
5. **Linh hoạt:** Có thể reset cấu hình khi cần

## 🎯 **Kết luận**

Với tính năng này, ESP32 sẽ:
- ✅ **Tự động lưu** thông tin WiFi vào flash memory
- ✅ **Tự động khôi phục** cấu hình sau khi mất nguồn
- ✅ **Tự động kết nối lại** khi mất WiFi
- ✅ **Hiển thị trạng thái** chi tiết trên Serial Monitor và LCD

**Không còn lo lắng về việc mất cấu hình WiFi!** 🚀
