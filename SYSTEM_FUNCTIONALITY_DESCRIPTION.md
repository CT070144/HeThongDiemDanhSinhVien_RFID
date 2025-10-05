# MÔ TẢ CHI TIẾT CHỨC NĂNG HỆ THỐNG RFID ATTENDANCE

## TỔNG QUAN HỆ THỐNG

Hệ thống RFID Attendance là một giải pháp điểm danh tự động sử dụng công nghệ RFID (Radio Frequency Identification) kết hợp với ESP32, Spring Boot backend và React frontend. Hệ thống cho phép sinh viên điểm danh bằng cách quét thẻ RFID và tự động ghi nhận thời gian vào/ra của từng ca học.

## KIẾN TRÚC HỆ THỐNG

### 1. PHẦN CỨNG (Hardware Layer)
- **ESP32 Development Board**: Vi điều khiển chính
- **RC522 RFID Module**: Module đọc thẻ RFID
- **DHT11 Sensor**: Cảm biến nhiệt độ và độ ẩm
- **OLED Display**: Màn hình OLED 128x64 0.96 inch I2C để hiển thị thông tin
- **LED đa sắc**: LED để hiển thị các trạng thái khác nhau
- **Buzzer**: Còi báo âm thanh cho các sự kiện
- **Button**: Nút nhấn để refresh thủ công
- **Breadboard và dây nối**: Kết nối các linh kiện

### 2. PHẦN MỀM (Software Layer)
- **ESP32 Firmware**: Code Arduino C++ chạy trên ESP32
- **Spring Boot Backend**: REST API server (Java)
- **React Frontend**: Web application (JavaScript)
- **MySQL Database**: Lưu trữ dữ liệu

## CHI TIẾT CHỨC NĂNG ESP32

### 1. KHỞI TẠO HỆ THỐNG (Setup Function)

```cpp
void setup() {
  // Khởi tạo Serial Monitor với baud rate 115200
  Serial.begin(115200);
  
  // Cấu hình các chân GPIO
  pinMode(LED_WIFI_SUCCESS, OUTPUT);    // LED xanh - WiFi thành công
  pinMode(LED_WIFI_FAIL, OUTPUT);        // LED đỏ - WiFi thất bại
  pinMode(LED_ATTENDANCE_SUCCESS, OUTPUT); // LED xanh lá - điểm danh thành công
  pinMode(LED_ATTENDANCE_FAIL, OUTPUT); // LED cam - điểm danh thất bại
  pinMode(LED_CARD_DETECTED, OUTPUT);  // LED xanh dương - phát hiện thẻ
  pinMode(BUTTON_PIN, INPUT_PULLUP);   // Nút nhấn
  pinMode(BUZZER_PIN, OUTPUT);         // Còi báo
  
  // Khởi tạo DHT11 sensor
  dht.begin();
  
  // Khởi tạo OLED Display
  display.begin(SSD1306_SWITCHCAPVCC, 0x3C);
  display.clearDisplay();
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);
  
  // Khởi tạo SPI và RFID module
  SPI.begin();
  mfrc522.PCD_Init();
  
  // Kết nối WiFi
  connectToWiFi();
  
  // Hiển thị thông tin RFID module
  mfrc522.PCD_DumpVersionToSerial();
  
  // Hiển thị màn hình khởi động
  displayStartupScreen();
}
```

**Chức năng:**
- Khởi tạo giao tiếp Serial để debug
- Cấu hình các chân GPIO cho LED, button, buzzer và sensor
- Khởi tạo cảm biến DHT11 để đo nhiệt độ và độ ẩm
- Khởi tạo màn hình OLED để hiển thị thông tin
- Khởi tạo giao tiếp SPI với module RC522
- Kết nối WiFi với thông tin đã cấu hình
- Kiểm tra và hiển thị thông tin RFID module
- Hiển thị màn hình khởi động trên OLED

### 2. VÒNG LẶP CHÍNH (Main Loop)

```cpp
void loop() {
  // 1. Kiểm tra kết nối WiFi
  if (WiFi.status() != WL_CONNECTED) {
    wifiConnected = false;
    digitalWrite(LED_WIFI_FAIL, HIGH);
    digitalWrite(LED_WIFI_SUCCESS, LOW);
    connectToWiFi();
  } else if (!wifiConnected) {
    wifiConnected = true;
    digitalWrite(LED_WIFI_FAIL, LOW);
    digitalWrite(LED_WIFI_SUCCESS, HIGH);
    Serial.println("WiFi reconnected!");
  }
  
  // 2. Cập nhật nhiệt độ và độ ẩm mỗi 5 giây
  if (millis() - lastTempUpdate > 5000) {
    updateTemperatureHumidity();
    lastTempUpdate = millis();
  }
  
  // 3. Hiển thị thông tin trên OLED (nhiệt độ, độ ẩm, trạng thái)
  updateOLEDDisplay();
  
  // 4. Kiểm tra nút nhấn thủ công
  if (digitalRead(BUTTON_PIN) == LOW) {
    delay(50); // Debounce
    if (digitalRead(BUTTON_PIN) == LOW) {
      Serial.println("Manual refresh triggered");
      playBuzzer(1, 200); // Báo hiệu bằng buzzer
      delay(1000);
    }
  }
  
  // 5. Đọc thẻ RFID
  if (mfrc522.PICC_IsNewCardPresent()) {
    if (mfrc522.PICC_ReadCardSerial()) {
      // Xử lý thẻ RFID
      String rfid = readRFID();
      if (rfid.length() > 0) {
        digitalWrite(LED_CARD_DETECTED, HIGH);
        playBuzzer(2, 150); // Báo hiệu phát hiện thẻ
        sendToServer(rfid);
        delay(500);
        digitalWrite(LED_CARD_DETECTED, LOW);
      }
    }
  }
}
```

**Chức năng:**
- Liên tục kiểm tra và duy trì kết nối WiFi với LED trạng thái
- Cập nhật nhiệt độ và độ ẩm từ cảm biến DHT11 định kỳ
- Hiển thị thông tin thời gian thực trên màn hình OLED
- Xử lý sự kiện nút nhấn thủ công với báo hiệu buzzer
- Phát hiện và đọc thẻ RFID mới với LED và buzzer báo hiệu
- Gửi dữ liệu lên server và xử lý response

### 3. ĐỌC THẺ RFID (RFID Reading)

```cpp
String readRFID() {
  String rfid = "";
  
  // Đọc UID của thẻ
  for (byte i = 0; i < mfrc522.uid.size; i++) {
    Serial.print(mfrc522.uid.uidByte[i] < 0x10 ? "0" : "");
    Serial.print(mfrc522.uid.uidByte[i], HEX);
    rfid += String(mfrc522.uid.uidByte[i] < 0x10 ? "0" : "");
    rfid += String(mfrc522.uid.uidByte[i], HEX);
  }
  
  rfid.toUpperCase();
  return rfid;
}
```

**Chức năng:**
- Đọc UID (Unique Identifier) của thẻ RFID
- Chuyển đổi từ byte array sang chuỗi hex
- Format chuỗi với padding 0 nếu cần
- Chuyển đổi sang chữ hoa để chuẩn hóa

### 4. GỬI DỮ LIỆU LÊN SERVER (HTTP POST)

```cpp
void sendToServer(String rfid) {
  HTTPClient http;
  http.begin(serverURL);
  http.addHeader("Content-Type", "application/json");
  
  // Tạo JSON payload
  DynamicJsonDocument doc(1024);
  doc["rfid"] = rfid;
  
  String jsonString;
  serializeJson(doc, jsonString);
  
  // Gửi POST request
  int httpResponseCode = http.POST(jsonString);
  
  // Xử lý response
  if (httpResponseCode == 200) {
    blinkLED(LED_GREEN, 2, 300); // Thành công
  } else {
    blinkLED(LED_RED, 2, 300);   // Lỗi
  }
}
```

**Chức năng:**
- Tạo HTTP client và kết nối đến server
- Tạo JSON payload chứa mã RFID
- Gửi POST request đến endpoint `/api/attendance/rfid`
- Xử lý response và hiển thị trạng thái bằng LED

### 5. KẾT NỐI WIFI (WiFi Connection)

```cpp
void connectToWiFi() {
  WiFi.begin(ssid, password);
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(1000);
    Serial.print(".");
    attempts++;
    
    // Nhấp nháy LED đỏ khi đang kết nối
    digitalWrite(LED_RED, !digitalRead(LED_RED));
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("WiFi connected successfully!");
    Serial.print("IP address: ");
    Serial.println(WiFi.localIP());
    wifiConnected = true;
    blinkLED(LED_GREEN, 2, 200);
  }
}
```

**Chức năng:**
- Kết nối WiFi với SSID và password đã cấu hình
- Hiển thị tiến trình kết nối trên Serial Monitor
- Nhấp nháy LED đỏ trong quá trình kết nối
- Hiển thị IP address khi kết nối thành công
- Báo hiệu thành công bằng LED xanh

### 6. QUẢN LÝ TRẠNG THÁI LED (LED Status Management)

### 4. CẢM BIẾN NHIỆT ĐỘ VÀ ĐỘ ẨM (DHT11 Sensor)

```cpp
void updateTemperatureHumidity() {
  // Đọc nhiệt độ và độ ẩm từ DHT11
  float humidity = dht.readHumidity();
  float temperature = dht.readTemperature();
  
  // Kiểm tra giá trị hợp lệ
  if (isnan(humidity) || isnan(temperature)) {
    Serial.println("Failed to read from DHT sensor!");
    return;
  }
  
  // Lưu giá trị vào biến global
  currentTemperature = temperature;
  currentHumidity = humidity;
  
  // In ra Serial Monitor
  Serial.print("Temperature: ");
  Serial.print(temperature);
  Serial.print("°C, Humidity: ");
  Serial.print(humidity);
  Serial.println("%");
}
```

**Chức năng:**
- Đọc nhiệt độ và độ ẩm từ cảm biến DHT11
- Kiểm tra tính hợp lệ của dữ liệu
- Lưu trữ giá trị vào biến global
- Hiển thị thông tin trên Serial Monitor
- Cập nhật định kỳ mỗi 5 giây

### 5. MÀN HÌNH OLED HIỂN THỊ THÔNG TIN

```cpp
void updateOLEDDisplay() {
  display.clearDisplay();
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);
  
  // Hiển thị trạng thái WiFi
  display.setCursor(0, 0);
  if (wifiConnected) {
    display.println("WiFi: Connected");
  } else {
    display.println("WiFi: Disconnected");
  }
  
  // Hiển thị nhiệt độ và độ ẩm
  display.setCursor(0, 12);
  display.print("Temp: ");
  display.print(currentTemperature);
  display.println("C");
  
  display.setCursor(0, 24);
  display.print("Humidity: ");
  display.print(currentHumidity);
  display.println("%");
  
  // Hiển thị thời gian hiện tại
  display.setCursor(0, 36);
  display.print("Time: ");
  display.println(getCurrentTime());
  
  // Hiển thị trạng thái hệ thống
  display.setCursor(0, 48);
  if (lastAttendanceStatus == "SUCCESS") {
    display.println("Status: Ready");
  } else if (lastAttendanceStatus == "FAILED") {
    display.println("Status: Error");
  } else {
    display.println("Status: Waiting...");
  }
  
  // Hiển thị thông tin sinh viên cuối cùng (nếu có)
  if (lastStudentName.length() > 0) {
    display.setCursor(0, 56);
    display.print("Last: ");
    display.println(lastStudentName);
  }
  
  display.display();
}

void displayStudentInfo(String studentName, String studentId, String status) {
  display.clearDisplay();
  display.setTextSize(2);
  display.setTextColor(SSD1306_WHITE);
  
  // Hiển thị tên sinh viên
  display.setCursor(0, 0);
  display.println(studentName);
  
  // Hiển thị mã sinh viên
  display.setTextSize(1);
  display.setCursor(0, 20);
  display.print("ID: ");
  display.println(studentId);
  
  // Hiển thị trạng thái điểm danh
  display.setCursor(0, 32);
  display.print("Status: ");
  display.println(status);
  
  // Hiển thị thời gian
  display.setCursor(0, 44);
  display.print("Time: ");
  display.println(getCurrentTime());
  
  display.display();
  
  // Giữ thông tin hiển thị trong 3 giây
  delay(3000);
}
```

**Chức năng:**
- Hiển thị trạng thái WiFi (kết nối/ngắt kết nối)
- Hiển thị nhiệt độ và độ ẩm thời gian thực
- Hiển thị thời gian hiện tại
- Hiển thị trạng thái hệ thống (sẵn sàng/lỗi/chờ)
- Hiển thị thông tin sinh viên khi điểm danh thành công
- Cập nhật màn hình liên tục với thông tin mới nhất

### 6. CÒI BÁO VÀ LED TRẠNG THÁI

```cpp
void playBuzzer(int times, int duration) {
  for (int i = 0; i < times; i++) {
    digitalWrite(BUZZER_PIN, HIGH);
    delay(duration);
    digitalWrite(BUZZER_PIN, LOW);
    delay(duration);
  }
}

void setLEDStatus(String status) {
  // Tắt tất cả LED trước
  digitalWrite(LED_WIFI_SUCCESS, LOW);
  digitalWrite(LED_WIFI_FAIL, LOW);
  digitalWrite(LED_ATTENDANCE_SUCCESS, LOW);
  digitalWrite(LED_ATTENDANCE_FAIL, LOW);
  digitalWrite(LED_CARD_DETECTED, LOW);
  
  if (status == "WIFI_SUCCESS") {
    digitalWrite(LED_WIFI_SUCCESS, HIGH);
    playBuzzer(1, 300);
  } else if (status == "WIFI_FAIL") {
    digitalWrite(LED_WIFI_FAIL, HIGH);
    playBuzzer(3, 200);
  } else if (status == "ATTENDANCE_SUCCESS") {
    digitalWrite(LED_ATTENDANCE_SUCCESS, HIGH);
    playBuzzer(2, 250);
  } else if (status == "ATTENDANCE_FAIL") {
    digitalWrite(LED_ATTENDANCE_FAIL, HIGH);
    playBuzzer(1, 500);
  } else if (status == "CARD_DETECTED") {
    digitalWrite(LED_CARD_DETECTED, HIGH);
    playBuzzer(1, 150);
  }
}
```

**Chức năng:**
- **LED WiFi Success (GPIO 2)**: WiFi kết nối thành công - 1 tiếng buzzer dài
- **LED WiFi Fail (GPIO 4)**: WiFi kết nối thất bại - 3 tiếng buzzer ngắn
- **LED Attendance Success (GPIO 5)**: Điểm danh thành công - 2 tiếng buzzer vừa
- **LED Attendance Fail (GPIO 18)**: Điểm danh thất bại - 1 tiếng buzzer dài
- **LED Card Detected (GPIO 19)**: Phát hiện thẻ RFID - 1 tiếng buzzer ngắn

## CẤU HÌNH PHẦN CỨNG

### Kết nối RC522 với ESP32:
```
RC522 Pin    ESP32 Pin    Chức năng
VCC      ->  3.3V        Nguồn điện
GND      ->  GND         Mass
RST      ->  GPIO 22     Reset
SS       ->  GPIO 21     Slave Select
MOSI     ->  GPIO 23     Master Out Slave In
MISO     ->  GPIO 19     Master In Slave Out
SCK      ->  GPIO 18     Serial Clock
```

### Kết nối DHT11 với ESP32:
```
DHT11 Pin     ESP32 Pin    Chức năng
VCC       ->  3.3V        Nguồn điện
GND       ->  GND         Mass
DATA      ->  GPIO 15     Data pin
```

### Kết nối OLED Display với ESP32:
```
OLED Pin      ESP32 Pin    Chức năng
VCC       ->  3.3V        Nguồn điện
GND       ->  GND         Mass
SDA       ->  GPIO 21     I2C Data
SCL       ->  GPIO 22     I2C Clock
```

### Cấu hình LED và Buzzer:
```
LED WiFi Success      -> GPIO 2   (Xanh - WiFi thành công)
LED WiFi Fail         -> GPIO 4   (Đỏ - WiFi thất bại)
LED Attendance Success -> GPIO 5   (Xanh lá - điểm danh thành công)
LED Attendance Fail   -> GPIO 18   (Cam - điểm danh thất bại)
LED Card Detected    -> GPIO 19   (Xanh dương - phát hiện thẻ)
Buzzer               -> GPIO 16   (Còi báo âm thanh)
Button               -> GPIO 0    (Refresh thủ công)
```

## CẤU HÌNH PHẦN MỀM

### 1. Thông tin WiFi:
```cpp
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";
```

### 2. Server URL:
```cpp
const char* serverURL = "http://192.168.1.100:8080/api/attendance/rfid";
```

### 3. Thời gian chờ và cấu hình:
```cpp
const unsigned long READ_INTERVAL = 2000;        // 2 giây giữa các lần đọc RFID
const unsigned long DUPLICATE_SEND_INTERVAL = 5000; // 5 giây cho phép gửi lại cùng RFID
const unsigned long TEMP_UPDATE_INTERVAL = 5000;   // 5 giây cập nhật nhiệt độ/độ ẩm
const unsigned long OLED_UPDATE_INTERVAL = 1000;    // 1 giây cập nhật OLED
const unsigned long STUDENT_DISPLAY_TIME = 3000;    // 3 giây hiển thị thông tin sinh viên
```

### 4. Cấu hình cảm biến DHT11:
```cpp
#define DHT_PIN 15
#define DHT_TYPE DHT11
DHT dht(DHT_PIN, DHT_TYPE);
```

### 5. Cấu hình OLED Display:
```cpp
#define OLED_RESET -1
Adafruit_SSD1306 display(128, 64, &Wire, OLED_RESET);
```

## LUỒNG HOẠT ĐỘNG CHI TIẾT

### 1. Khởi động hệ thống:
1. ESP32 khởi động và khởi tạo các module
2. Khởi tạo cảm biến DHT11 và OLED display
3. Kết nối WiFi và hiển thị trạng thái trên OLED
4. Khởi tạo RFID module
5. Hiển thị màn hình khởi động với thông tin hệ thống
6. Bắt đầu cập nhật nhiệt độ/độ ẩm định kỳ

### 2. Chế độ chờ (Standby Mode):
1. Hiển thị nhiệt độ và độ ẩm thời gian thực trên OLED
2. Hiển thị trạng thái WiFi và thời gian hiện tại
3. Cập nhật màn hình OLED mỗi giây
4. Cập nhật cảm biến DHT11 mỗi 5 giây
5. Kiểm tra kết nối WiFi liên tục

### 3. Đọc thẻ RFID:
1. Phát hiện thẻ RFID trong vùng đọc
2. Bật LED Card Detected và phát buzzer ngắn
3. Đọc UID của thẻ và chuyển đổi sang hex string
4. Kiểm tra thời gian chờ giữa các lần đọc
5. Hiển thị "Reading Card..." trên OLED

### 4. Gửi dữ liệu và xử lý response:
1. Tạo JSON payload chứa mã RFID
2. Gửi HTTP POST request đến server
3. Hiển thị "Sending..." trên OLED
4. Xử lý response từ server:
   - **Thành công**: Bật LED Attendance Success, buzzer 2 tiếng, hiển thị thông tin sinh viên
   - **Thất bại**: Bật LED Attendance Fail, buzzer 1 tiếng dài, hiển thị lỗi
5. Giữ thông tin sinh viên hiển thị trong 3 giây
6. Quay về chế độ chờ

### 5. Xử lý lỗi và phục hồi:
1. Kiểm tra kết nối WiFi liên tục
2. Tự động kết nối lại khi mất WiFi với LED và buzzer báo hiệu
3. Hiển thị lỗi trên OLED và Serial Monitor
4. Retry mechanism cho HTTP requests
5. Graceful error handling cho tất cả module

## TÍNH NĂNG ĐẶC BIỆT

### 1. Chống đọc trùng lặp:
- Có thời gian chờ 2 giây giữa các lần đọc RFID
- Cho phép gửi lại cùng RFID sau 5 giây (cho luồng đăng ký rồi điểm danh)

### 2. Quản lý thẻ thông minh:
- Chờ thẻ được rút ra trước khi cho phép đọc lại
- Reset biến `lastRfid` sau khi thẻ được rút ra
- Hiển thị trạng thái đọc thẻ trên OLED

### 3. Hiển thị thông tin đa dạng:
- Màn hình OLED hiển thị nhiệt độ/độ ẩm thời gian thực
- Hiển thị thông tin sinh viên khi điểm danh thành công
- Trạng thái WiFi và hệ thống được cập nhật liên tục
- Thời gian hiện tại được hiển thị chính xác

### 4. Báo hiệu đa phương thức:
- 5 LED với màu sắc khác nhau cho từng trạng thái
- Buzzer với âm thanh khác nhau cho từng sự kiện
- OLED hiển thị thông tin chi tiết
- Serial Monitor log đầy đủ

### 5. Debug và monitoring nâng cao:
- Serial Monitor với baud rate 115200
- Hiển thị chi tiết thông tin WiFi, nhiệt độ, độ ẩm
- Log tất cả hoạt động và lỗi
- OLED hiển thị trạng thái real-time

### 6. Tự động phục hồi và xử lý lỗi:
- Tự động kết nối lại WiFi khi mất kết nối
- Retry mechanism cho HTTP requests
- Graceful error handling cho tất cả sensor
- Fallback display khi có lỗi sensor

## TÍCH HỢP VỚI BACKEND

### API Endpoint:
- **URL**: `POST /api/attendance/rfid`
- **Payload**: `{"rfid": "A1B2C3D4"}`
- **Response**: Thông tin phiếu điểm danh hoặc lỗi

### Xử lý phía server:
1. Nhận mã RFID từ ESP32
2. Tìm sinh viên tương ứng trong database
3. Nếu không tìm thấy, lưu vào bảng `docrfid1` để xử lý sau
4. Nếu tìm thấy, tạo/cập nhật phiếu điểm danh
5. Trả về kết quả cho ESP32

## MONITORING VÀ DEBUG

### Serial Monitor Output:
```
RFID Attendance System Starting...
Initializing DHT11 sensor...
Initializing OLED display...
Connecting to WiFi: YOUR_WIFI_SSID
WiFi connected successfully!
IP address: 192.168.1.150
Signal strength: -45 dBm
Temperature: 25.3°C, Humidity: 60.2%
System ready! Place RFID card near the reader.
UID tag: A1B2C3D4
RFID detected: A1B2C3D4
Reading card...
Sending to server: {"rfid":"A1B2C3D4"}
HTTP Response Code: 200
Server Response: {"id":123,"rfid":"A1B2C3D4","studentName":"Nguyen Van A",...}
Attendance recorded successfully!
Displaying student info for 3 seconds...
Temperature: 25.4°C, Humidity: 60.1%
```

### LED Status Indicators:
- **LED WiFi Success (GPIO 2)**: WiFi kết nối thành công - 1 tiếng buzzer dài
- **LED WiFi Fail (GPIO 4)**: WiFi kết nối thất bại - 3 tiếng buzzer ngắn
- **LED Attendance Success (GPIO 5)**: Điểm danh thành công - 2 tiếng buzzer vừa
- **LED Attendance Fail (GPIO 18)**: Điểm danh thất bại - 1 tiếng buzzer dài
- **LED Card Detected (GPIO 19)**: Phát hiện thẻ RFID - 1 tiếng buzzer ngắn

### OLED Display Information:
- **Dòng 1**: Trạng thái WiFi (Connected/Disconnected)
- **Dòng 2**: Nhiệt độ hiện tại (Temp: XX.X°C)
- **Dòng 3**: Độ ẩm hiện tại (Humidity: XX.X%)
- **Dòng 4**: Thời gian hiện tại (Time: HH:MM:SS)
- **Dòng 5**: Trạng thái hệ thống (Ready/Error/Waiting...)
- **Dòng 6**: Thông tin sinh viên cuối cùng (Last: Name)

## TROUBLESHOOTING

### 1. ESP32 không kết nối WiFi:
- Kiểm tra SSID và password
- Đảm bảo ESP32 và máy tính cùng mạng
- Kiểm tra Serial Monitor để xem lỗi chi tiết

### 2. RFID không đọc được thẻ:
- Kiểm tra kết nối phần cứng RC522
- Đảm bảo thẻ RFID hoạt động
- Kiểm tra Serial Monitor để debug

### 3. Không gửi được dữ liệu lên server:
- Kiểm tra IP address server trong code
- Đảm bảo backend đang chạy
- Kiểm tra firewall và network

### 4. OLED không hiển thị:
- Kiểm tra kết nối I2C (SDA, SCL)
- Kiểm tra địa chỉ I2C (thường là 0x3C)
- Kiểm tra điện áp cung cấp 3.3V
- Kiểm tra code khởi tạo OLED

### 5. DHT11 không đọc được dữ liệu:
- Kiểm tra kết nối DATA pin
- Đảm bảo điện áp cung cấp 3.3V
- Kiểm tra pull-up resistor nếu cần
- Kiểm tra Serial Monitor để xem lỗi chi tiết

### 6. Buzzer không hoạt động:
- Kiểm tra kết nối GPIO buzzer
- Kiểm tra điện áp cung cấp
- Kiểm tra code điều khiển buzzer

## MỞ RỘNG VÀ TỐI ƯU

### Tính năng có thể thêm:
- Camera để nhận diện khuôn mặt
- Nhiều RFID reader cho nhiều cửa
- Battery backup với UPS
- OTA (Over-The-Air) updates
- WebSocket cho real-time communication
- GPS module để tracking vị trí
- SD card để lưu trữ offline
- Touch screen thay thế OLED
- Voice announcement system
- QR code scanner integration

### Tối ưu hiệu suất:
- Deep sleep mode để tiết kiệm điện
- Caching RFID data và sensor data
- Compression cho HTTP requests
- Error recovery mechanisms
- Multi-threading cho các task độc lập
- Power management cho các module
- Data logging và analytics

Hệ thống RFID Attendance nâng cao này cung cấp một giải pháp điểm danh tự động hoàn chỉnh với nhiều tính năng hiện đại:

## **TÍNH NĂNG CHÍNH:**
- **Điểm danh RFID tự động** với RC522 module
- **Hiển thị thông tin đa dạng** trên OLED 128x64
- **Giám sát môi trường** với cảm biến DHT11 (nhiệt độ/độ ẩm)
- **Báo hiệu đa phương thức** với 5 LED và buzzer
- **Kết nối WiFi** với tự động reconnect
- **Tích hợp backend** Spring Boot và frontend React

## **ƯU ĐIỂM:**
- **User-friendly**: Hiển thị thông tin sinh viên khi điểm danh
- **Real-time monitoring**: Nhiệt độ/độ ẩm cập nhật liên tục
- **Robust error handling**: Xử lý lỗi và phục hồi tự động
- **Multi-sensory feedback**: LED + buzzer + OLED display
- **Scalable architecture**: Dễ dàng mở rộng và tùy chỉnh

Hệ thống này phù hợp cho các ứng dụng điểm danh trong trường học, văn phòng, hoặc bất kỳ môi trường nào cần theo dõi sự có mặt của người dùng một cách tự động và thông minh.
