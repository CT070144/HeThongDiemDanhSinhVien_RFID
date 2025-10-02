# Sơ đồ kết nối phần cứng ESP32 + RC522

## Kết nối RC522 với ESP32

```
RC522 RFID Module    ESP32 Development Board
┌─────────────────┐   ┌─────────────────┐
│ VCC             │───│ 3.3V           │
│ GND             │───│ GND             │
│ RST             │───│ GPIO 22        │
│ SS (SDA)        │───│ GPIO 21        │
│ MOSI            │───│ GPIO 23        │
│ MISO            │───│ GPIO 19        │
│ SCK             │───│ GPIO 18        │
│ IRQ             │───│ (Không kết nối) │
└─────────────────┘   └─────────────────┘
```

## Kết nối LED và Button

```
Component          ESP32 Pin
┌─────────────┐   ┌─────────────┐
│ LED Green   │───│ GPIO 2      │
│ LED Red     │───│ GPIO 4      │
│ LED Blue    │───│ GPIO 5      │
│ Button      │───│ GPIO 0      │
└─────────────┘   └─────────────┘
```

## Sơ đồ mạch đầy đủ

```
                    ESP32 Dev Board
                    ┌─────────────┐
                    │             │
                    │ 3.3V ───────┼── VCC (RC522)
                    │ GND  ───────┼── GND (RC522)
                    │ GPIO22 ─────┼── RST (RC522)
                    │ GPIO21 ─────┼── SS (RC522)
                    │ GPIO23 ─────┼── MOSI (RC522)
                    │ GPIO19 ─────┼── MISO (RC522)
                    │ GPIO18 ─────┼── SCK (RC522)
                    │             │
                    │ GPIO2  ─────┼── LED Green (+)
                    │ GPIO4  ─────┼── LED Red (+)
                    │ GPIO5  ─────┼── LED Blue (+)
                    │ GPIO0  ─────┼── Button (Pull-up)
                    │             │
                    └─────────────┘
```

## Lưu ý quan trọng

1. **Điện áp**: RC522 hoạt động ở 3.3V, không dùng 5V
2. **Pull-up**: Button cần có pull-up resistor hoặc sử dụng INPUT_PULLUP
3. **LED**: Cần có resistor hạn dòng cho LED (220Ω - 1kΩ)
4. **Antenna**: Đảm bảo antenna của RC522 không bị che khuất
5. **Khoảng cách**: RC522 đọc được thẻ trong khoảng 2-5cm

## Test phần cứng

1. **Test LED**: Upload code test LED đơn giản
2. **Test Button**: Kiểm tra button có hoạt động không
3. **Test RC522**: Upload code test RFID đơn giản
4. **Test WiFi**: Kiểm tra kết nối WiFi
5. **Test Server**: Kiểm tra giao tiếp với server

## Code test đơn giản

```cpp
// Test LED
void setup() {
  pinMode(2, OUTPUT);
  pinMode(4, OUTPUT);
  pinMode(5, OUTPUT);
}

void loop() {
  digitalWrite(2, HIGH); delay(500);
  digitalWrite(4, HIGH); delay(500);
  digitalWrite(5, HIGH); delay(500);
  digitalWrite(2, LOW); delay(500);
  digitalWrite(4, LOW); delay(500);
  digitalWrite(5, LOW); delay(500);
}
```

## Troubleshooting phần cứng

- **RC522 không đọc được**: Kiểm tra kết nối SPI, điện áp 3.3V
- **LED không sáng**: Kiểm tra kết nối và resistor
- **Button không hoạt động**: Kiểm tra pull-up resistor
- **ESP32 không boot**: Kiểm tra nguồn điện và kết nối
