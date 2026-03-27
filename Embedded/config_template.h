/*
 * ESP32 RFID Attendance System - Configuration Template
 * 
 * Copy this file and rename to config.h
 * Update the values below according to your setup
 */

#ifndef CONFIG_H
#define CONFIG_H

// ===========================================
// WIFI CONFIGURATION
// ===========================================
const char* WIFI_SSID = "YOUR_WIFI_NAME_HERE";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD_HERE";

// ===========================================
// SERVER CONFIGURATION
// ===========================================
// Find your computer's IP address:
// Windows: ipconfig
// Linux/Mac: ifconfig
const char* SERVER_IP = "192.168.1.100";  // CHANGE THIS TO YOUR COMPUTER'S IP
const int SERVER_PORT = 8080;
const char* API_ENDPOINT = "/api/attendance/rfid";

// Full server URL will be: http://192.168.1.100:8080/api/attendance/rfid

// ===========================================
// HARDWARE PIN CONFIGURATION
// ===========================================
// RC522 RFID Module pins
#define RFID_RST_PIN 22
#define RFID_SS_PIN 21

// LED pins (with current limiting resistors)
#define LED_GREEN_PIN 2   // Success/Ready
#define LED_RED_PIN 4     // Error/WiFi issues
#define LED_BLUE_PIN 5    // Status/Info

// Button pin (with pull-up resistor)
#define BUTTON_PIN 0      // Manual refresh button

// ===========================================
// TIMING CONFIGURATION
// ===========================================
const unsigned long RFID_READ_INTERVAL = 2000;        // 2 seconds between RFID reads
const unsigned long WIFI_RECONNECT_INTERVAL = 30000;  // 30 seconds between WiFi reconnection attempts
const unsigned long SERVER_TIMEOUT = 10000;            // 10 seconds server request timeout

// ===========================================
// SYSTEM CONFIGURATION
// ===========================================
const int SERIAL_BAUD_RATE = 115200;
const bool ENABLE_DEBUG = true;  // Set to false for production

// ===========================================
// CA HOC CONFIGURATION (in minutes from midnight)
// ===========================================
const int CA1_START = 420;   // 07:00 (7*60)
const int CA1_END = 570;     // 09:30 (9*60 + 30)
const int CA2_START = 570;  // 09:30
const int CA2_END = 720;     // 12:00 (12*60)
const int CA3_START = 750;  // 12:30 (12*60 + 30)
const int CA3_END = 900;     // 15:00 (15*60)
const int CA4_START = 900;  // 15:00
const int CA4_END = 1050;   // 17:30 (17*60 + 30)

// Late threshold (minutes after ca start)
const int LATE_THRESHOLD = 15;  // 15 minutes late

// ===========================================
// LED STATUS CODES
// ===========================================
#define LED_STATUS_WIFI_CONNECTING 1
#define LED_STATUS_WIFI_CONNECTED 2
#define LED_STATUS_WIFI_ERROR 3
#define LED_STATUS_RFID_READ 4
#define LED_STATUS_SERVER_SUCCESS 5
#define LED_STATUS_SERVER_ERROR 6
#define LED_STATUS_SYSTEM_READY 7

// ===========================================
// DEBUG MESSAGES
// ===========================================
#define DEBUG_WIFI "WiFi"
#define DEBUG_RFID "RFID"
#define DEBUG_SERVER "Server"
#define DEBUG_SYSTEM "System"

#endif // CONFIG_H
