# Hướng dẫn sửa lỗi thư viện LiquidCrystal_I2C

## 🚨 Vấn đề
Thư viện `LiquidCrystal_I2C` hiện tại được thiết kế cho AVR architecture và có thể không tương thích hoàn toàn với ESP32.

## 🔧 Giải pháp 1: Cài đặt thư viện tương thích ESP32

### Bước 1: Gỡ cài đặt thư viện cũ
1. Mở Arduino IDE
2. Vào **Tools** → **Manage Libraries**
3. Tìm kiếm "LiquidCrystal I2C"
4. Gỡ cài đặt thư viện hiện tại

### Bước 2: Cài đặt thư viện mới
Tìm kiếm và cài đặt một trong các thư viện sau:

**Option A: LiquidCrystal_I2C by Marco Schwartz**
- Tìm kiếm: "LiquidCrystal_I2C"
- Tác giả: Marco Schwartz
- Hỗ trợ ESP32

**Option B: LiquidCrystal_I2C by Frank de Brabander**
- Tìm kiếm: "LiquidCrystal_I2C"
- Tác giả: Frank de Brabander
- Phiên bản mới hỗ trợ ESP32

## 🔧 Giải pháp 2: Sử dụng thư viện thay thế

Nếu vẫn gặp vấn đề, có thể thay thế bằng:

```cpp
#include <Wire.h>
#include <LiquidCrystal_I2C_ESP32.h>

// Thay đổi khởi tạo LCD
LiquidCrystal_I2C_ESP32 lcd(LCD_ADDRESS, LCD_COLUMNS, LCD_ROWS);
```

## 🔧 Giải pháp 3: Sử dụng thư viện ESP32 native

```cpp
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

// Khởi tạo LCD với cú pháp ESP32
LiquidCrystal_I2C lcd(LCD_ADDRESS, 16, 2); // Address, columns, rows
```

## 📋 Kiểm tra sau khi cài đặt

1. **Compile code** để kiểm tra lỗi
2. **Upload lên ESP32** để test
3. **Kiểm tra Serial Monitor** để xem có lỗi không

## 🚨 Lưu ý quan trọng

- **Địa chỉ I2C** của LCD thường là `0x27` hoặc `0x3F`
- **SDA pin**: GPIO 21 (ESP32)
- **SCL pin**: GPIO 22 (ESP32)
- **VCC**: 3.3V hoặc 5V
- **GND**: Ground

## 🔍 Troubleshooting

### Lỗi "LCD not found"
1. Kiểm tra kết nối dây
2. Kiểm tra địa chỉ I2C bằng I2C Scanner
3. Kiểm tra nguồn điện

### Lỗi "I2C error"
1. Kiểm tra pull-up resistors
2. Kiểm tra tốc độ I2C (thử 100kHz)
3. Kiểm tra kết nối SDA/SCL

## 📚 Code I2C Scanner (để tìm địa chỉ LCD)

```cpp
#include <Wire.h>

void setup() {
  Wire.begin();
  Serial.begin(115200);
  Serial.println("I2C Scanner");
}

void loop() {
  byte error, address;
  int nDevices;
  Serial.println("Scanning...");
  
  nDevices = 0;
  for(address = 1; address < 127; address++ ) {
    Wire.beginTransmission(address);
    error = Wire.endTransmission();
    
    if (error == 0) {
      Serial.print("I2C device found at address 0x");
      if (address<16) Serial.print("0");
      Serial.print(address,HEX);
      Serial.println(" !");
      nDevices++;
    }
  }
  
  if (nDevices == 0) Serial.println("No I2C devices found");
  else Serial.println("done");
  
  delay(5000);
}
```

Sau khi sửa các lỗi này, code sẽ compile thành công!
