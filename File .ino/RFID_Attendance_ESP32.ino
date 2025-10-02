/*
 * RFID Attendance System - ESP32 Code
 * 
 * Hardware Requirements:
 * - ESP32 Development Board
 * - RC522 RFID Module
 * - Breadboard and jumper wires
 * 
 * Connections:
 * RC522    ESP32
 * VCC  ->  3.3V
 * GND  ->  GND
 * RST  ->  GPIO 22
 * SS   ->  GPIO 21
 * MOSI ->  GPIO 23
 * MISO ->  GPIO 19
 * SCK  ->  GPIO 18
 * 
 * Features:
 * - WiFi connection
 * - RFID card reading
 * - HTTP POST to Spring Boot server
 * - LED indicators for status
 * - Serial monitor debugging
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <SPI.h>
#include <MFRC522.h>
#include <ArduinoJson.h>

// WiFi credentials
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

// Server configuration
const char* serverURL = "http://192.168.1.100:8080/api/attendance/rfid";
// Change the IP address to your computer's IP address where Spring Boot is running

// RFID configuration
#define RST_PIN 22
#define SS_PIN 21

// LED pins
#define LED_GREEN 2
#define LED_RED 4
#define LED_BLUE 5

// Button pin for manual refresh
#define BUTTON_PIN 0

MFRC522 mfrc522(SS_PIN, RST_PIN);

// Variables
String lastRfid = "";
unsigned long lastReadTime = 0;
const unsigned long READ_INTERVAL = 2000; // 2 seconds between reads
// Allow re-sending the same RFID after this cooldown (for register then attend flow)
const unsigned long DUPLICATE_SEND_INTERVAL = 5000; // 5 seconds
bool wifiConnected = false;

void setup() {
  Serial.begin(115200);
  
  // Initialize pins
  pinMode(LED_GREEN, OUTPUT);
  pinMode(LED_RED, OUTPUT);
  pinMode(LED_BLUE, OUTPUT);
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  
  // Initialize LEDs
  digitalWrite(LED_GREEN, LOW);
  digitalWrite(LED_RED, LOW);
  digitalWrite(LED_BLUE, LOW);
  
  // Initialize SPI and RFID
  SPI.begin();
  mfrc522.PCD_Init();
  
  Serial.println("RFID Attendance System Starting...");
  Serial.println("Initializing RFID module...");
  
  // Show RFID module info
  mfrc522.PCD_DumpVersionToSerial();
  
  // Connect to WiFi
  connectToWiFi();
  
  Serial.println("System ready! Place RFID card near the reader.");
  blinkLED(LED_BLUE, 3, 200); // Blue LED blinks 3 times to indicate ready
}

void loop() {
  // Check WiFi connection
  if (WiFi.status() != WL_CONNECTED) {
    wifiConnected = false;
    digitalWrite(LED_RED, HIGH);
    Serial.println("WiFi disconnected. Attempting to reconnect...");
    connectToWiFi();
  } else if (!wifiConnected) {
    wifiConnected = true;
    digitalWrite(LED_RED, LOW);
    Serial.println("WiFi reconnected!");
  }
  
  // Check for button press (manual refresh)
  if (digitalRead(BUTTON_PIN) == LOW) {
    delay(50); // Debounce
    if (digitalRead(BUTTON_PIN) == LOW) {
      Serial.println("Manual refresh triggered");
      blinkLED(LED_BLUE, 2, 100);
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
    
    // Send to server
    sendToServer(rfid);
    
    // Blink green LED to indicate successful read
    blinkLED(LED_GREEN, 1, 500);
  }
  
  // Halt PICC
  mfrc522.PICC_HaltPICC();
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
  lastRfid = "";
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
    blinkLED(LED_RED, 3, 200);
    return;
  }
  
  HTTPClient http;
  http.begin(serverURL);
  http.addHeader("Content-Type", "application/json");
  
  // Create JSON payload
  DynamicJsonDocument doc(1024);
  doc["rfid"] = rfid;
  
  String jsonString;
  serializeJson(doc, jsonString);
  
  Serial.println("Sending to server: " + jsonString);
  
  // Send POST request
  int httpResponseCode = http.POST(jsonString);
  
  if (httpResponseCode > 0) {
    String response = http.getString();
    Serial.println("HTTP Response Code: " + String(httpResponseCode));
    Serial.println("Server Response: " + response);
    
    if (httpResponseCode == 200) {
      Serial.println("Attendance recorded successfully!");
      blinkLED(LED_GREEN, 2, 300);
    } else {
      Serial.println("Server error: " + String(httpResponseCode));
      blinkLED(LED_RED, 2, 300);
    }
  } else {
    Serial.println("Error sending request: " + String(httpResponseCode));
    blinkLED(LED_RED, 3, 200);
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
    
    // Blink red LED while connecting
    digitalWrite(LED_RED, !digitalRead(LED_RED));
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
    digitalWrite(LED_RED, LOW);
    
    // Blink green LED to indicate successful connection
    blinkLED(LED_GREEN, 2, 200);
  } else {
    Serial.println("");
    Serial.println("Failed to connect to WiFi!");
    wifiConnected = false;
    digitalWrite(LED_RED, HIGH);
  }
}

void blinkLED(int pin, int times, int delayMs) {
  for (int i = 0; i < times; i++) {
    digitalWrite(pin, HIGH);
    delay(delayMs);
    digitalWrite(pin, LOW);
    delay(delayMs);
  }
}

void printWiFiStatus() {
  Serial.println("=== WiFi Status ===");
  Serial.println("SSID: " + WiFi.SSID());
  Serial.println("IP Address: " + WiFi.localIP().toString());
  Serial.println("Signal Strength: " + String(WiFi.RSSI()) + " dBm");
  Serial.println("MAC Address: " + WiFi.macAddress());
  Serial.println("==================");
}
