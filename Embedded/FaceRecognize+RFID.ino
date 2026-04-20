/*
 * RFID Attendance System - ESP32 Code with WiFiManager
 * 
 * Required Libraries:
 * - MFRC522 (for RFID)
 * - ArduinoJson (for JSON handling)
 * - LiquidCrystal_I2C (for LCD1602 display)
 * - WiFiManager (for WiFi configuration)
 * - Preferences (for storing settings in flash)
 * 
 * Hardware Requirements:
 * - ESP32 Development Board
 * - RC522 RFID Module
 * - LCD1602 I2C Display
 * - Buzzer (2 cực SFM-27)
 * - Button (for config reset)
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
 * Button          ESP32
 * One side    ->  GPIO 0
 * Other side  ->  GND
 * 
 * Features:
 * - WiFiManager for easy WiFi configuration via web portal
 * - Persistent storage of WiFi credentials and device settings
 * - RFID card reading
 * - HTTP POST to Spring Boot server with configurable device ID
 * - LCD1602 display for status messages
 * - Buzzer audio feedback:
 *   * Single beep when RFID is scanned
 *   * Long beep when attendance is successful
 *   * Double beep when there's an error
 * - Button control:
 *   * Hold button on startup: Clear saved WiFi credentials
 *   * Hold button for 5s during operation: Reset all configuration
 *   * Short press: Refresh display
 * - Serial monitor debugging
 * - Configurable device ID and server URL
 * - Automatic reconnection to WiFi
 * - NTP time synchronization
 */

 #include <WiFi.h>
 #include <HTTPClient.h>
 #include <SPI.h>
 #include <MFRC522.h>
 #include <ArduinoJson.h>
 #include <LiquidCrystal_I2C.h>
 #include <time.h>
 #include <WiFiManager.h>
 #include <Preferences.h>
// FreeRTOS
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "freertos/semphr.h"
 
 // WiFi credentials (sẽ được lưu vào flash)
 String ssid = "";
 String password = "";
 
// Server configuration
String serverURL = "http://192.168.1.70:8080/api/attendance/rfid";
// Change the IP address to your computer's IP address where Spring Boot is running

// Device configuration (sẽ được lưu vào flash)
String DEVICE_ID = "DEVICE_001";
String API_KEY = ""; // API key cho ESP32

// WiFiManager instance
WiFiManager wm;

// Preferences để lưu cấu hình vào flash
Preferences preferences;

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
 
// Button pin for manual refresh and config reset
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
const unsigned long DUPLICATE_SEND_INTERVAL = 3000; // 2 seconds
const unsigned long TIME_UPDATE_INTERVAL = 1000; // Update time every 1 second
bool wifiConnected = false;
bool isDisplayingMessage = false; // Flag to prevent time update during message display
bool shouldSaveConfig = false; // Flag for saving config

// ===========================
// FreeRTOS Queues & Events
// ===========================
typedef struct {
  char uid[32];
  uint32_t timestamp;
} RfidEvent_t;

typedef enum { UI_INFO, UI_SUCCESS, UI_FAIL, UI_ERROR } UiType_t;
typedef struct {
  UiType_t type;
  char line1[32];
  char line2[32];
  uint32_t displayMs;
} UiEvent_t;

QueueHandle_t qRFID;   // RfidEvent_t
QueueHandle_t qUI;     // UiEvent_t
QueueHandle_t qNET;    // RfidEvent_t (network upload)

// Custom parameters for WiFiManager
WiFiManagerParameter custom_device_id("device_id", "Device ID", "DEVICE_999", 20);
WiFiManagerParameter custom_server_url("server_url", "Server URL", "http://172.20.10.4:8080/api/attendance/rfid", 100);
WiFiManagerParameter custom_api_key("api_key", "API Key", "apitestkey", 64);
 
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
   Serial.println("Initializing RFID module...");
   
   // Show RFID module info
   mfrc522.PCD_DumpVersionToSerial();
   
  // Load saved configuration
  loadConfig();
  
  // Check WiFi configuration status
  checkWiFiConfigStatus();
  
  // Setup WiFiManager
  setupWiFiManager();
   
   Serial.println("System ready! Place RFID card near the reader.");
   
   // Display ready message on LCD
   lcd.setCursor(0, 0);
   lcd.print("Sync time...");
   lcd.setCursor(0, 1);
   lcd.print("RFID Ready!");

   // Configure time after WiFi connection
   setupTime();
  // ===========================
  // Create FreeRTOS Queues
  // ===========================
  qRFID = xQueueCreate(8, sizeof(RfidEvent_t));
  qNET  = xQueueCreate(8, sizeof(RfidEvent_t));
  qUI   = xQueueCreate(8, sizeof(UiEvent_t));

  // ===========================
  // Create FreeRTOS Tasks
  // ===========================
  xTaskCreatePinnedToCore(TaskRFID,     "TaskRFID",     4096, NULL, 5, NULL, 1);
  xTaskCreatePinnedToCore(TaskNet,      "TaskNet",      6144, NULL, 3, NULL, 0);
  xTaskCreatePinnedToCore(TaskUI,       "TaskUI",       4096, NULL, 2, NULL, 0);
  xTaskCreatePinnedToCore(TaskWatchdog, "TaskWatchdog", 4096, NULL, 1, NULL, 0);
}

// ===========================
// FreeRTOS Tasks
// ===========================
void TaskRFID(void* pv) {
  UiEvent_t ui;
  for (;;) {
    // Debounce button for config reset short press UI refresh
    if (digitalRead(BUTTON_PIN) == LOW) {
      vTaskDelay(pdMS_TO_TICKS(50));
      if (digitalRead(BUTTON_PIN) == LOW) {
        // Check hold 5s for reset
        unsigned long start = millis();
        while (digitalRead(BUTTON_PIN) == LOW && (millis() - start) < 5000) {
          vTaskDelay(pdMS_TO_TICKS(100));
        }
        if (millis() - start >= 5000) {
          preferences.begin("rfid_config", false);
          preferences.clear();
          preferences.end();
          WiFi.disconnect(true);
          snprintf(ui.line1, sizeof ui.line1, "Config Reset");
          snprintf(ui.line2, sizeof ui.line2, "Restarting...");
          ui.type = UI_INFO; ui.displayMs = 2000;
          xQueueSend(qUI, &ui, 0);
          vTaskDelay(pdMS_TO_TICKS(500));
          ESP.restart();
        } else {
          snprintf(ui.line1, sizeof ui.line1, "Display Refresh");
          snprintf(ui.line2, sizeof ui.line2, "Ready to scan");
          ui.type = UI_INFO; ui.displayMs = 1000;
          xQueueSend(qUI, &ui, 0);
        }
      }
    }

    if (!mfrc522.PICC_IsNewCardPresent()) {
      vTaskDelay(pdMS_TO_TICKS(50));
      continue;
    }
    if (!mfrc522.PICC_ReadCardSerial()) {
      vTaskDelay(pdMS_TO_TICKS(20));
      continue;
    }
    if (millis() - lastReadTime < READ_INTERVAL) {
      // drain card
      mfrc522.PICC_HaltA();
      mfrc522.PCD_StopCrypto1();
      vTaskDelay(pdMS_TO_TICKS(50));
      continue;
    }
    // Build UID
    char uid[32] = {0};
    for (byte i = 0; i < mfrc522.uid.size; i++) {
      char hex[4];
      // %02X: print 2 digits hex number, padding with 0 if needed
      snprintf(hex, sizeof(hex), "%02X", mfrc522.uid.uidByte[i]);
      strncat(uid, hex, sizeof(uid) - strlen(uid) - 1);
    }
    mfrc522.PICC_HaltA();
    mfrc522.PCD_StopCrypto1();

    String rfidStr = String(uid);
    if (rfidStr.length() > 0 && (rfidStr != lastRfid || (millis() - lastReadTime > DUPLICATE_SEND_INTERVAL))) {
      lastRfid = rfidStr;
      lastReadTime = millis();

      // UI: show scanning + beep
      beepScan();
      snprintf(ui.line1, sizeof ui.line1, "RFID: %.16s", uid);
      snprintf(ui.line2, sizeof ui.line2, "Sending...");
      ui.type = UI_INFO; ui.displayMs = 1000;
      xQueueSend(qUI, &ui, 0);

      // Enqueue to NET
      RfidEvent_t evt;
      strncpy(evt.uid, uid, sizeof evt.uid);
      evt.timestamp = (uint32_t)time(nullptr);
      xQueueSend(qNET, &evt, 0);
    }
  }
}

void TaskNet(void* pv) {
  RfidEvent_t evt;
  for (;;) {
    if (xQueueReceive(qNET, &evt, portMAX_DELAY) == pdTRUE) {
      sendToServer(String(evt.uid));
    }
  }
}

void TaskUI(void* pv) {
  UiEvent_t evt;
  unsigned long lastTick = 0;
  for (;;) {
    // Periodic clock refresh when idle
    if (xQueueReceive(qUI, &evt, pdMS_TO_TICKS(50)) == pdTRUE) {
      // Display message
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print(evt.line1);
      lcd.setCursor(0, 1);
      lcd.print(evt.line2);
      if (evt.type == UI_SUCCESS) { beepSuccess(); }
      else if (evt.type == UI_FAIL || evt.type == UI_ERROR) { beepError(); }
      if (evt.displayMs > 0) {
        vTaskDelay(pdMS_TO_TICKS(evt.displayMs));
      }
    } else {
      if (millis() - lastTick > TIME_UPDATE_INTERVAL) {
        lastTick = millis();
        lcd.clear();
        lcd.setCursor(0, 0);
        lcd.print(getCurrentDate() + String(" ") + getCurrentTime());
        lcd.setCursor(0, 1);
        lcd.print("RFID Ready!");
      }
    }
  }
}

void TaskWatchdog(void* pv) {
  UiEvent_t ui;
  for (;;) {
    if (WiFi.status() != WL_CONNECTED) {
      wifiConnected = false;
      snprintf(ui.line1, sizeof ui.line1, "WiFi Lost");
      snprintf(ui.line2, sizeof ui.line2, "Reconnecting...");
      ui.type = UI_INFO; ui.displayMs = 1000;
      xQueueSend(qUI, &ui, 0);

      if (ssid != "" && password != "") {
        WiFi.begin(ssid.c_str(), password.c_str());
        int attempts = 0;
        while (WiFi.status() != WL_CONNECTED && attempts++ < 10) {
          vTaskDelay(pdMS_TO_TICKS(1000));
        }
        if (WiFi.status() == WL_CONNECTED) {
          wifiConnected = true;
          snprintf(ui.line1, sizeof ui.line1, "WiFi OK");
          snprintf(ui.line2, sizeof ui.line2, "%s", WiFi.localIP().toString().c_str());
          ui.type = UI_INFO; ui.displayMs = 1500;
          xQueueSend(qUI, &ui, 0);
        } else {
          snprintf(ui.line1, sizeof ui.line1, "Reconnect Fail");
          snprintf(ui.line2, sizeof ui.line2, "Check WiFi");
          ui.type = UI_ERROR; ui.displayMs = 1500;
          xQueueSend(qUI, &ui, 0);
        }
      } else {
        snprintf(ui.line1, sizeof ui.line1, "No WiFi Config");
        snprintf(ui.line2, sizeof ui.line2, "Hold BTN 5s");
        ui.type = UI_INFO; ui.displayMs = 1500;
        xQueueSend(qUI, &ui, 0);
      }
    } else if (!wifiConnected) {
      wifiConnected = true;
      snprintf(ui.line1, sizeof ui.line1, "WiFi Connected!");
      snprintf(ui.line2, sizeof ui.line2, "Scan card...");
      ui.type = UI_INFO; ui.displayMs = 1500;
      xQueueSend(qUI, &ui, 0);
    }
    vTaskDelay(pdMS_TO_TICKS(3000));
  }
}
 
void loop() {
  vTaskDelay(pdMS_TO_TICKS(1000));
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
  // Check real-time WiFi status to avoid relying on a stale flag
  if (WiFi.status() != WL_CONNECTED) {
   Serial.println("WiFi not connected. Cannot send data to server.");
   Serial.println("WiFi.status(): " + String(WiFi.status()));
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
   WiFiClient wifiClient;
   // Defensive timeouts to reduce stalls on unreachable server
   http.setConnectTimeout(5000); // ms
   http.setTimeout(8000);        // ms
   http.begin(wifiClient, serverURL.c_str());
   http.addHeader("Content-Type", "application/json");
   
   // Thêm API key vào header nếu có
   if (API_KEY.length() > 0) {
     http.addHeader("X-API-Key", API_KEY);
   }
   
  // Create JSON payload
  DynamicJsonDocument doc(1024);
  doc["rfid"] = rfid;
  doc["maThietBi"] = DEVICE_ID.c_str();
   
  String jsonString;
  serializeJson(doc, jsonString);
  
  Serial.println("Sending to server: " + jsonString);
  Serial.println("POST URL: " + serverURL);
   
   // Send POST request
   int httpResponseCode = http.POST(jsonString);
   
  if (httpResponseCode > 0) {
     String response = http.getString();
     Serial.println("HTTP Response Code: " + String(httpResponseCode));
     Serial.println("Server Response: " + response);
     
     // Kiểm tra nếu API key không hợp lệ
     if (httpResponseCode == 401) {
       isDisplayingMessage = true;
       lcd.clear();
       lcd.setCursor(0, 0);
       lcd.print("API KEY ERROR");
       lcd.setCursor(0, 1);
       lcd.print("CHECK CONFIG");
       beepError();
       delay(3000);
       isDisplayingMessage = false;
       http.end();
       return;
     }
     
     // Parse JSON response regardless of HTTP status code
     DynamicJsonDocument responseDoc(1024);
     DeserializationError error = deserializeJson(responseDoc, response);
     
     if (!error) {
       // JSON parsing successful
       String status = responseDoc["status"] | "";
       String studentName = responseDoc["name"] | "";
       
       Serial.println("Status: " + status);
       Serial.println("Name: " + studentName);
       
       // Yêu cầu hiển thị theo spec:
       // - HTTP 200 + status Face_required: hiển thị status lên LCD
       // - HTTP 200 + status not_found: báo INVALID CARD
       // - HTTP != 200: SERVER ERROR
       isDisplayingMessage = true;
       if (httpResponseCode == 200) {
         if (status == "Face_required") {
           lcd.clear();
           lcd.setCursor(0, 0);
           lcd.print("FACE REQUIRED");
           beepScan();
           delay(1500);
         } else if (status == "not_found") {
           lcd.clear();
           lcd.setCursor(0, 0);
           lcd.print("CARD NOT FOUND");
           beepError();
           delay(3000);
         } else {
           lcd.clear();
           lcd.setCursor(0, 0);
           lcd.print("UNKNOWN STATUS");
           lcd.setCursor(0, 1);
           lcd.print(status.substring(0, 16));
           beepError();
           delay(2000);
         }
       } else {
         lcd.clear();
         lcd.setCursor(0, 0);
         lcd.print("SERVER ERROR");
         lcd.setCursor(0, 1);
         lcd.print("HTTP " + String(httpResponseCode));
         beepError();
         delay(2000);
       }
       isDisplayingMessage = false;
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
    Serial.println("HTTPClient error: " + HTTPClient::errorToString(httpResponseCode));
    Serial.println("Ensure server at: " + serverURL);
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
 
 // Legacy connectToWiFi function - now handled by setupWiFiManager
 void connectToWiFi() {
   // This function is now replaced by setupWiFiManager()
   // Kept for compatibility but not used
   Serial.println("connectToWiFi() called - use setupWiFiManager() instead");
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

// Function to check and display WiFi configuration status
void checkWiFiConfigStatus() {
  Serial.println("=== WiFi Configuration Status ===");
  Serial.println("Saved SSID: " + String(ssid.length() > 0 ? ssid.c_str() : "[NOT SAVED]"));
  Serial.println("Saved Password: " + String(password.length() > 0 ? "[SAVED]" : "[NOT SAVED]"));
  Serial.println("Current SSID: " + WiFi.SSID());
  Serial.println("WiFi Status: " + String(WiFi.status()));
  Serial.println("Connection Status: " + String(wifiConnected ? "CONNECTED" : "DISCONNECTED"));
  
  // Check if saved credentials match current connection
  if (ssid.length() > 0 && WiFi.SSID() == ssid) {
    Serial.println("✓ Saved credentials match current connection");
  } else if (ssid.length() > 0) {
    Serial.println("⚠ Saved credentials don't match current connection");
  } else {
    Serial.println("⚠ No WiFi credentials saved");
  }
  Serial.println("=====================================");
}

// Save configuration to flash memory
void saveConfig() {
  preferences.begin("rfid_config", false);
  preferences.putString("ssid", ssid);
  preferences.putString("password", password);
  preferences.putString("device_id", DEVICE_ID);
  preferences.putString("server_url", serverURL);
  preferences.putString("api_key", API_KEY);
  preferences.putBool("wifi_saved", true); // Flag to indicate WiFi is saved
  preferences.end();
  Serial.println("Configuration saved to flash");
  Serial.println("SSID: " + ssid);
  Serial.println("Password: [HIDDEN]");
  Serial.println("Device ID: " + DEVICE_ID);
  Serial.println("Server URL: " + serverURL);
  Serial.println("API Key: " + String(API_KEY.length() > 0 ? "[SAVED]" : "[NOT SET]"));
}

// Load configuration from flash memory
void loadConfig() {
  preferences.begin("rfid_config", false);
  ssid = preferences.getString("ssid", "");
  password = preferences.getString("password", "");
  DEVICE_ID = preferences.getString("device_id", "DEVICE_001");
  serverURL = preferences.getString("server_url", "http://192.168.1.70:8080/api/attendance/rfid");
  API_KEY = preferences.getString("api_key", "");
  bool wifiSaved = preferences.getBool("wifi_saved", false);
  preferences.end();
  
  Serial.println("Configuration loaded from flash:");
  Serial.println("SSID: " + ssid);
  Serial.println("Password: " + String(password.length() > 0 ? "[SAVED]" : "[NOT SAVED]"));
  Serial.println("Device ID: " + DEVICE_ID);
  Serial.println("Server URL: " + serverURL);
  Serial.println("API Key: " + String(API_KEY.length() > 0 ? "[SAVED]" : "[NOT SAVED]"));
  Serial.println("WiFi Saved Flag: " + String(wifiSaved ? "YES" : "NO"));
}

// Callback function for saving configuration
void saveConfigCallback() {
  Serial.println("Should save config");
  shouldSaveConfig = true;
}

// Setup WiFiManager with custom parameters
void setupWiFiManager() {
  // Check if button is pressed on startup (reset config)
  if (digitalRead(BUTTON_PIN) == LOW) {
    Serial.println("Button pressed on startup - entering config mode");
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Config Mode");
    lcd.setCursor(0, 1);
    lcd.print("Hold button...");
    delay(3000);
    
    if (digitalRead(BUTTON_PIN) == LOW) {
      Serial.println("Clearing saved WiFi credentials");
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("Clearing WiFi");
      lcd.setCursor(0, 1);
      lcd.print("credentials...");
      
      // Clear saved credentials
      preferences.begin("rfid_config", false);
      preferences.clear();
      preferences.end();
      
      // Reset WiFi settings
      WiFi.disconnect(true);
      delay(1000);
    }
  }
  
  // Set save config callback
  wm.setSaveConfigCallback(saveConfigCallback);
  
  // Add custom parameters
  wm.addParameter(&custom_device_id);
  wm.addParameter(&custom_server_url);
  wm.addParameter(&custom_api_key);
  
  // Set custom hostname
  wm.setHostname("RFID-Device");
  
  // Set timeout for config portal (3 minutes)
  wm.setConfigPortalTimeout(180);
  
  // Display on LCD
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("WiFi Setup");
  lcd.setCursor(0, 1);
  lcd.print("Starting...");
  
  // Try to connect with saved credentials
  if (ssid != "" && password != "") {
    Serial.println("Trying to connect with saved credentials...");
    Serial.println("SSID: " + ssid);
    Serial.println("Password: [SAVED]");
    
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Connecting to");
    lcd.setCursor(0, 1);
    lcd.print(ssid);
    
    // Set WiFi mode to station
    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid.c_str(), password.c_str());
    
    int attempts = 0;
    const int maxAttempts = 15; // Increase attempts for better reliability
    
    while (WiFi.status() != WL_CONNECTED && attempts < maxAttempts) {
      delay(1000);
      Serial.print(".");
      attempts++;
      
      // Update LCD every 3 attempts
      if (attempts % 3 == 0) {
        lcd.clear();
        lcd.setCursor(0, 0);
        lcd.print("Connecting...");
        lcd.setCursor(0, 1);
        lcd.print("Attempt " + String(attempts));
      }
    }
    
    if (WiFi.status() == WL_CONNECTED) {
      wifiConnected = true;
      Serial.println("");
      Serial.println("WiFi connected with saved credentials!");
      Serial.print("IP address: ");
      Serial.println(WiFi.localIP());
      Serial.print("Signal strength: ");
      Serial.print(WiFi.RSSI());
      Serial.println(" dBm");
      
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("WiFi Connected!");
      lcd.setCursor(0, 1);
      lcd.print(WiFi.localIP().toString());
      delay(2000);
      return;
    } else {
      Serial.println("");
      Serial.println("Failed to connect with saved credentials after " + String(maxAttempts) + " attempts");
      Serial.println("WiFi Status: " + String(WiFi.status()));
    }
  } else {
    Serial.println("No saved WiFi credentials found");
  }
  
  // If connection failed, start config portal
  Serial.println("Failed to connect with saved credentials. Starting config portal...");
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Starting Config");
  lcd.setCursor(0, 1);
  lcd.print("Portal...");
  
  // Start config portal
  if (!wm.startConfigPortal("RFID-Device", "12345678")) {
    Serial.println("Failed to connect and hit timeout");
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Config Timeout");
    lcd.setCursor(0, 1);
    lcd.print("Restarting...");
    delay(3000);
    ESP.restart();
  }
  
  // If we get here, we are connected
  wifiConnected = true;
  Serial.println("Connected to WiFi!");
  
  // Save custom parameters and WiFi credentials
  if (shouldSaveConfig) {
    // Update device settings
    DEVICE_ID = String(custom_device_id.getValue());
    serverURL = String(custom_server_url.getValue());
    API_KEY = String(custom_api_key.getValue());
    
    // Update WiFi credentials from current connection
    ssid = WiFi.SSID();
    password = WiFi.psk();
    
    // Save everything to flash
    saveConfig();
    shouldSaveConfig = false;
    
    Serial.println("=== Configuration Saved Successfully ===");
    Serial.println("WiFi SSID: " + ssid);
    Serial.println("WiFi Password: [SAVED]");
    Serial.println("Device ID: " + DEVICE_ID);
    Serial.println("Server URL: " + serverURL);
    Serial.println("API Key: " + String(API_KEY.length() > 0 ? "[SAVED]" : "[NOT SET]"));
    Serial.println("=========================================");
  }
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("WiFi Connected!");
  lcd.setCursor(0, 1);
  lcd.print(WiFi.localIP().toString());
  delay(2000);
}
 