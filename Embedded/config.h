/*
 * ESP32 RFID Attendance System - Configuration File
 * 
 * Instructions for setup:
 * 
 * 1. HARDWARE CONNECTIONS:
 *    RC522 RFID Module -> ESP32
 *    VCC  -> 3.3V
 *    GND  -> GND
 *    RST  -> GPIO 22
 *    SS   -> GPIO 21
 *    MOSI -> GPIO 23
 *    MISO -> GPIO 19
 *    SCK  -> GPIO 18
 * 
 * 2. SOFTWARE SETUP:
 *    - Install ESP32 board package in Arduino IDE
 *    - Install required libraries:
 *      * MFRC522 by GithubCommunity
 *      * ArduinoJson by Benoit Blanchon
 *    - Update WiFi credentials in the main .ino file
 *    - Update server URL with your computer's IP address
 * 
 * 3. NETWORK CONFIGURATION:
 *    - Make sure ESP32 and computer are on the same network
 *    - Find your computer's IP address (ipconfig on Windows, ifconfig on Linux/Mac)
 *    - Update serverURL variable with the correct IP address
 * 
 * 4. TESTING:
 *    - Upload the code to ESP32
 *    - Open Serial Monitor (115200 baud)
 *    - Check WiFi connection status
 *    - Test with RFID cards
 * 
 * 5. TROUBLESHOOTING:
 *    - Red LED: WiFi connection issues
 *    - Green LED: Successful RFID read and server communication
 *    - Blue LED: System ready/status indicator
 *    - Check Serial Monitor for detailed error messages
 */

// WiFi Configuration - UPDATE THESE VALUES
const char* WIFI_SSID = "YOUR_WIFI_NAME";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

// Server Configuration - UPDATE WITH YOUR COMPUTER'S IP
const char* SERVER_IP = "192.168.1.100";  // Change this to your computer's IP
const int SERVER_PORT = 8080;
const char* API_ENDPOINT = "/api/attendance/rfid";

// Hardware Pin Configuration
#define RFID_RST_PIN 22
#define RFID_SS_PIN 21
#define LED_GREEN_PIN 2
#define LED_RED_PIN 4
#define LED_BLUE_PIN 5
#define BUTTON_PIN 0

// Timing Configuration
const unsigned long RFID_READ_INTERVAL = 2000;  // 2 seconds between reads
const unsigned long WIFI_RECONNECT_INTERVAL = 30000;  // 30 seconds between reconnection attempts

// LED Status Codes
#define LED_STATUS_WIFI_CONNECTING 1
#define LED_STATUS_WIFI_CONNECTED 2
#define LED_STATUS_WIFI_ERROR 3
#define LED_STATUS_RFID_READ 4
#define LED_STATUS_SERVER_SUCCESS 5
#define LED_STATUS_SERVER_ERROR 6
#define LED_STATUS_SYSTEM_READY 7

// Function prototypes for LED control
void setLEDStatus(int status);
void blinkLED(int pin, int times, int delayMs);
void updateSystemStatus();

// Network functions
bool connectToWiFi();
void checkWiFiConnection();
String getServerURL();

// RFID functions
String readRFIDCard();
bool isNewCardPresent();

// Server communication
bool sendAttendanceData(String rfid);
String createJSONPayload(String rfid);
int parseServerResponse(String response);

// Utility functions
void printSystemInfo();
void printWiFiStatus();
void printRFIDInfo();
