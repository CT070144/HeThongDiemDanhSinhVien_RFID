/*
 * RFID Attendance System - ESP32 Code
 * 
 * Required Libraries:
 * - MFRC522 (for RFID)
 * - ArduinoJson (for JSON handling)
 * - LiquidCrystal_I2C (for LCD1602 display)
 * 
 * Hardware Requirements:
 * - ESP32 Development Board
 * - RC522 RFID Module
 * - LCD1602 I2C Display
 * - Buzzer (2 cực SFM-27)
 * - Breadboard and jumper wires
 * 
 * Connections:
 * RC522    ESP32
 * VCC  ->  VIN (hoặc 3.3V)
 * GND  ->  GND
 * RST  ->  GPIO 4
 * SS   ->  GPIO 5
 * MOSI ->  GPIO 23
 * MISO ->  GPIO 19
 * SCK  ->  GPIO 18
 * 
 * LCD1602 I2C    ESP32
 * VCC  ->  3.3V hoặc 5V
 * GND  ->  GND
 * SDA  ->  GPIO 21 (default I2C SDA)
 * SCL  ->  GPIO 22 (default I2C SCL)
 * 
 * Buzzer SFM-27   ESP32
 * + (Positive) -> GPIO 15
 * - (Negative) -> GND
 * 
 * Features:
 * - WiFi connection
 * - RFID card reading
 * - HTTP POST to Spring Boot server with device ID
 * - LCD1602 display for status messages
 * - Buzzer audio feedback:
 *   * Single beep when RFID is scanned
 *   * Long beep when attendance is successful
 *   * Double beep when there's an error
 * - Serial monitor debugging
 * - Device identification (DEVICE_001)
 */

 #include <WiFi.h>
 #include <HTTPClient.h>
 #include <SPI.h>
 #include <MFRC522.h>
 #include <ArduinoJson.h>
 #include <LiquidCrystal_I2C.h>
 #include <time.h>
 
 // WiFi credentials
 const char* ssid = "Ocngonduyenhai";
 const char* password = "camonquykhach";
 
// Server configuration
const char* serverURL = "http://192.168.1.70:8080/api/attendance/rfid";
// Change the IP address to your computer's IP address where Spring Boot is running

// Device configuration
const char* DEVICE_ID = "DEVICE_001";

// Time configuration
const char* ntpServer = "pool.ntp.org";
const long gmtOffset_sec = 7 * 3600; // GMT+7 cho Việt Nam
const int daylightOffset_sec = 0;
 
 // RFID configuration (đổi pins để tránh xung đột với I2C)
 #define RST_PIN 4
 #define SS_PIN 5
 
 // LCD configuration (I2C)
 #define LCD_ADDRESS 0x27  // I2C address of LCD (usually 0x27 or 0x3F)
 #define LCD_COLUMNS 16
 #define LCD_ROWS 2
 
 // Button pin for manual refresh
 #define BUTTON_PIN 0
 
 // Buzzer pin
 #define BUZZER_PIN 15
 
 MFRC522 mfrc522(SS_PIN, RST_PIN);
 LiquidCrystal_I2C lcd(LCD_ADDRESS, LCD_COLUMNS, LCD_ROWS);
 
// Variables
String lastRfid = "";
unsigned long lastReadTime = 0;
unsigned long lastTimeUpdate = 0;
const unsigned long READ_INTERVAL = 2000; // 2 seconds between reads
// Allow re-sending the same RFID after this cooldown (for register then attend flow)
const unsigned long DUPLICATE_SEND_INTERVAL = 15000; // 15 seconds
const unsigned long TIME_UPDATE_INTERVAL = 1000; // Update time every 1 second
bool wifiConnected = false;
bool isDisplayingMessage = false; // Flag to prevent time update during message display
 
 void setup() {
   Serial.begin(115200);
   
   // Initialize pins
   pinMode(BUTTON_PIN, INPUT_PULLUP);
   pinMode(BUZZER_PIN, OUTPUT);
   
   // Initialize buzzer (turn off initially)
   digitalWrite(BUZZER_PIN, LOW);
   
   // Initialize LCD
   lcd.init();
   lcd.backlight();
   lcd.clear();
   
   // Initialize SPI and RFID
   SPI.begin();
   mfrc522.PCD_Init();
   
  Serial.println("RFID Attendance System Starting...");
  Serial.println("Device ID: " + String(DEVICE_ID));
  Serial.println("Initializing RFID module...");
   
   // Show RFID module info
   mfrc522.PCD_DumpVersionToSerial();
   
   // Connect to WiFi
   lcd.clear();
   lcd.setCursor(0, 0);
   lcd.print("Connecting to");
   lcd.setCursor(0, 1);
   lcd.print("WiFi...");
   connectToWiFi();
   
   Serial.println("System ready! Place RFID card near the reader.");
   
  // Display ready message on LCD
  lcd.setCursor(0, 0);
  lcd.print("Sync time...");
  lcd.setCursor(0, 1);
  lcd.print("RFID Ready!");
 
  
  // Configure time after WiFi connection
  setupTime();
 }
 
void loop() {
  // Update time display every second
  if (millis() - lastTimeUpdate > TIME_UPDATE_INTERVAL && !isDisplayingMessage) {
    lastTimeUpdate = millis();
    
    // Chỉ cập nhật thời gian khi không có hoạt động RFID và không đang hiển thị thông báo
    if (!mfrc522.PICC_IsNewCardPresent()) {
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print(getCurrentDate() + " " + getCurrentTime());
      lcd.setCursor(0, 1);
      lcd.print("RFID Ready!");
   
    }
  }
  
  // Check WiFi connection
  if (WiFi.status() != WL_CONNECTED) {
     wifiConnected = false;
     lcd.clear();
     lcd.setCursor(0, 0);
     lcd.print("WiFi Disconnected");
     lcd.setCursor(0, 1);
     lcd.print("Reconnecting...");
     Serial.println("WiFi disconnected. Attempting to reconnect...");
     connectToWiFi();
   } else if (!wifiConnected) {
     wifiConnected = true;
     lcd.clear();
     lcd.setCursor(0, 0);
     lcd.print("WiFi Connected!");
     lcd.setCursor(0, 1);
     lcd.print("Scan card...");
     Serial.println("WiFi reconnected!");
     delay(2000);
   }
   
   // Check for button press (manual refresh)
   if (digitalRead(BUTTON_PIN) == LOW) {
     delay(50); // Debounce
     if (digitalRead(BUTTON_PIN) == LOW) {
       Serial.println("Manual refresh triggered");
       lcd.clear();
       lcd.setCursor(0, 0);
       lcd.print("Manual Refresh");
       lcd.setCursor(0, 1);
       lcd.print("Ready to scan");
       delay(1000);
     }
   }
   
   // Look for new cards
   if (!mfrc522.PICC_IsNewCardPresent()) {
     return;
   }
   
   // Select one of the cards
   if (!mfrc522.PICC_ReadCardSerial()) {
     return;
   }
   
   // Check if enough time has passed since last read
   if (millis() - lastReadTime < READ_INTERVAL) {
     return;
   }
   
   // Read RFID card
   String rfid = readRFID();
   
   if (rfid.length() > 0 && (rfid != lastRfid || (millis() - lastReadTime > DUPLICATE_SEND_INTERVAL))) {
     lastRfid = rfid;
     lastReadTime = millis();
     
     Serial.println("RFID detected: " + rfid);
     
     // Beep when RFID is scanned
     beepScan();
     
     // Display RFID on LCD
     lcd.clear();
     lcd.setCursor(0, 0);
     lcd.print("RFID: " + rfid);
     lcd.setCursor(0, 1);
     lcd.print("Sending...");
     
     // Send to server
     sendToServer(rfid);
   }
   
   // Halt PICC
   mfrc522.PICC_HaltA();
   // Stop encryption on PCD
   mfrc522.PCD_StopCrypto1();
 
   // Wait until card is removed to allow re-scan of the same UID
   // This ensures a second tap after registration will trigger a new send
   unsigned long removalStart = millis();
   while (mfrc522.PICC_IsNewCardPresent() || mfrc522.PICC_ReadCardSerial()) {
     delay(50);
     // safety break after 3s to avoid blocking loop indefinitely
     if (millis() - removalStart > 3000) {
       break;
     }
   }
   // Reset lastRfid so the same card can be sent again after removal
  
   
  // Return to ready state on LCD with time
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("RFID Ready!");
  lcd.setCursor(0, 1);
  lcd.print(getCurrentDate() + " " + getCurrentTime());
 }
 
 String readRFID() {
   String rfid = "";
   
   // Show UID on serial monitor
   Serial.print("UID tag: ");
   for (byte i = 0; i < mfrc522.uid.size; i++) {
     Serial.print(mfrc522.uid.uidByte[i] < 0x10 ? "0" : "");
     Serial.print(mfrc522.uid.uidByte[i], HEX);
     rfid += String(mfrc522.uid.uidByte[i] < 0x10 ? "0" : "");
     rfid += String(mfrc522.uid.uidByte[i], HEX);
   }
   Serial.println();
   rfid.toUpperCase();
   
   return rfid;
 }
 
 void sendToServer(String rfid) {
  if (!wifiConnected) {
    Serial.println("WiFi not connected. Cannot send data to server.");
    isDisplayingMessage = true;
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("NO WIFI");
    lcd.setCursor(0, 1);
    lcd.print("CHECK CONNECTION");
    beepError(); // Error beep for no WiFi
    delay(2000);
    isDisplayingMessage = false;
    return;
  }
   
   HTTPClient http;
   http.begin(serverURL);
   http.addHeader("Content-Type", "application/json");
   
  // Create JSON payload
  DynamicJsonDocument doc(1024);
  doc["rfid"] = rfid;
  doc["maThietBi"] = DEVICE_ID;
   
   String jsonString;
   serializeJson(doc, jsonString);
   
   Serial.println("Sending to server: " + jsonString);
   
   // Send POST request
   int httpResponseCode = http.POST(jsonString);
   
   if (httpResponseCode > 0) {
     String response = http.getString();
     Serial.println("HTTP Response Code: " + String(httpResponseCode));
     Serial.println("Server Response: " + response);
     
     // Parse JSON response regardless of HTTP status code
     DynamicJsonDocument responseDoc(1024);
     DeserializationError error = deserializeJson(responseDoc, response);
     
     if (!error) {
       // JSON parsing successful
       String status = responseDoc["status"] | "";
       String studentName = responseDoc["name"] | "";
       
       Serial.println("Status: " + status);
       Serial.println("Name: " + studentName);
       
       if (status == "found") {
         // Hiển thị tên sinh viên khi tìm thấy
         isDisplayingMessage = true;
         lcd.clear();
         lcd.setCursor(0, 0);
         lcd.print("SUCCESSFULLY!");
         lcd.setCursor(0, 1);
         lcd.print(studentName);
         beepSuccess(); // Success beep
         delay(2000);
         isDisplayingMessage = false;
       } else if (status == "not_found") {
         // Hiển thị thông báo không hợp lệ
         isDisplayingMessage = true;
         lcd.clear();
         lcd.setCursor(0, 0);
         lcd.print("CARD NOT FOUND");
         lcd.setCursor(0, 1);
         lcd.print("INVALID CARD");
         beepError(); // Error beep
         delay(3000);
         isDisplayingMessage = false;
       } else {
         // Trường hợp khác hoặc không có status
         isDisplayingMessage = true;
         if (httpResponseCode == 200) {
           Serial.println("Unknown status: " + status);
           lcd.clear();
           lcd.setCursor(0, 0);
           lcd.print("UNKNOWN STATUS");
           lcd.setCursor(0, 1);
           lcd.print(status);
         } else {
           Serial.println("Server error with status: " + status);
           lcd.clear();
           lcd.setCursor(0, 0);
           lcd.print("SERVER ERROR");
           lcd.setCursor(0, 1);
           lcd.print("HTTP " + String(httpResponseCode));
         }
         beepError();
         delay(2000);
         isDisplayingMessage = false;
       }
     } else {
       // JSON parsing failed
       isDisplayingMessage = true;
       Serial.println("Failed to parse JSON: " + String(error.c_str()));
       Serial.println("Server error: " + String(httpResponseCode));
       lcd.clear();
       lcd.setCursor(0, 0);
       lcd.print("SERVER ERROR");
       lcd.setCursor(0, 1);
       lcd.print("HTTP " + String(httpResponseCode));
       beepError(); // Error beep for server error
       delay(2000);
       isDisplayingMessage = false;
     }
   } else {
     isDisplayingMessage = true;
     Serial.println("Error sending request: " + String(httpResponseCode));
     lcd.clear();
     lcd.setCursor(0, 0);
     lcd.print("SEND ERROR");
     lcd.setCursor(0, 1);
     lcd.print("CHECK SERVER");
     beepError(); // Error beep for send error
     delay(2000);
     isDisplayingMessage = false;
   }
   
   http.end();
 }
 
 void connectToWiFi() {
   Serial.println("Connecting to WiFi: " + String(ssid));
   
   WiFi.begin(ssid, password);
   
   int attempts = 0;
   while (WiFi.status() != WL_CONNECTED && attempts < 20) {
     delay(1000);
     Serial.print(".");
     attempts++;
     
     // Display connecting progress on LCD
     lcd.clear();
     lcd.setCursor(0, 0);
     lcd.print("Connecting...");
     lcd.setCursor(0, 1);
     lcd.print("Attempt " + String(attempts));
   }
   
   if (WiFi.status() == WL_CONNECTED) {
     Serial.println("");
     Serial.println("WiFi connected successfully!");
     Serial.print("IP address: ");
     Serial.println(WiFi.localIP());
     Serial.print("Signal strength: ");
     Serial.print(WiFi.RSSI());
     Serial.println(" dBm");
     
     wifiConnected = true;
     
     // Display success message on LCD
     lcd.clear();
     lcd.setCursor(0, 0);
     lcd.print("WiFi Connected!");
     lcd.setCursor(0, 1);
     lcd.print(WiFi.localIP().toString());
     delay(2000);
   } else {
     Serial.println("");
     Serial.println("Failed to connect to WiFi!");
     wifiConnected = false;
     lcd.clear();
     lcd.setCursor(0, 0);
     lcd.print("WiFi Failed!");
     lcd.setCursor(0, 1);
     lcd.print("Check credentials");
     delay(2000);
   }
 }
 
 // Function to display message on LCD with timeout
 void displayMessage(String line1, String line2, int timeoutMs) {
   lcd.clear();
   lcd.setCursor(0, 0);
   lcd.print(line1);
   lcd.setCursor(0, 1);
   lcd.print(line2);
   if (timeoutMs > 0) {
     delay(timeoutMs);
   }
 }
 
 // Buzzer functions
 void beepScan() {
   // Single short beep when RFID is scanned
   digitalWrite(BUZZER_PIN, HIGH);
   delay(200);
   digitalWrite(BUZZER_PIN, LOW);
 }
 
 void beepSuccess() {
   // Long beep when attendance is successful
   digitalWrite(BUZZER_PIN, HIGH);
   delay(1000);
   digitalWrite(BUZZER_PIN, LOW);
 }
 
 void beepError() {
   // Two short beeps when there's an error
   for (int i = 0; i < 2; i++) {
     digitalWrite(BUZZER_PIN, HIGH);
     delay(500);
     digitalWrite(BUZZER_PIN, LOW);
     delay(100);
   }
 }
 
void setupTime() {
  Serial.println("Configuring time...");
  configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);
  
  // Chờ đồng bộ thời gian
  time_t now = 0;
  struct tm timeinfo = { 0 };
  int retry = 0;
  const int retry_count = 10;
  
  while(timeinfo.tm_year < (2024 - 1900) && ++retry < retry_count) {
    delay(2000);
    time(&now);
    localtime_r(&now, &timeinfo);
    Serial.println("Waiting for time sync...");
  }
  
  if (retry >= retry_count) {
    Serial.println("Failed to get time from NTP server");
  } else {
    Serial.println("Time synchronized successfully");
    Serial.println(&timeinfo, "%A, %B %d %Y %H:%M:%S");
  }
}

String getCurrentDate() {
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo)) {
    return "Date Error";
  }
  
  char dateString[12];
  strftime(dateString, sizeof(dateString), "%d/%m/%Y", &timeinfo);
  return String(dateString);
}

String getCurrentTime() {
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo)) {
    return "Time Error";
  }
  
  char timeString[8];
  strftime(timeString, sizeof(timeString), "%H:%M", &timeinfo);
  return String(timeString);
}

void printWiFiStatus() {
  Serial.println("=== WiFi Status ===");
  Serial.println("SSID: " + WiFi.SSID());
  Serial.println("IP Address: " + WiFi.localIP().toString());
  Serial.println("Signal Strength: " + String(WiFi.RSSI()) + " dBm");
  Serial.println("MAC Address: " + WiFi.macAddress());
  Serial.println("==================");
}
 