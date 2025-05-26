// Serial config
#define SerialMon Serial
#define SerialAT Serial1

// GSM / GPRS
#define TINY_GSM_MODEM_SIM7000
#define TINY_GSM_RX_BUFFER 1024
#define GSM_PIN "0000"
const char apn[] = "internet.tele2.lt";
const char gprsUser[] = "wap";
const char gprsPass[] = "wap";

// Hardcoded per-device identity info
const char* deviceId = "dev_001a2b";
const char* claimCode = "X7PLQ9"; // printed on box
const char* deviceSecret = "b12f98a3ce0f1b93490df"; // keep private!

bool deviceRegistered = false;



#include <TinyGsmClient.h>
#include <SPI.h>
#include <SD.h>
#include <Ticker.h>
#include <ESP32Servo.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <Wire.h>

Adafruit_MPU6050 mpu;

// Pins
#define UART_BAUD 9600
#define PIN_DTR 25
#define PIN_TX 27
#define PIN_RX 26
#define PWR_PIN 4
#define SD_MISO 2
#define SD_MOSI 15
#define SD_CS 13
#define LED_PIN 12
#define SERVO_PIN 14

// GSM modem instance
TinyGsm modem(SerialAT);

// Servo
Servo lockServo;

// Lock state
String currentLockState = "";
unsigned long lastLockCheck = 0;
const unsigned long lockPollInterval = 10000; // 10 sec
unsigned long lastGpsTime = 0;

void enableGPS() {
  modem.sendAT("+CGPIO=0,48,1,1");
  modem.waitResponse(10000L);
  modem.enableGPS();
}

void disableGPS() {
  modem.sendAT("+CGPIO=0,48,1,0");
  modem.waitResponse(10000L);
  modem.disableGPS();
}

void modemPowerOn() {
  pinMode(PWR_PIN, OUTPUT);
  digitalWrite(PWR_PIN, HIGH);
  delay(1000);
  digitalWrite(PWR_PIN, LOW);
}

void modemPowerOff() {
  pinMode(PWR_PIN, OUTPUT);
  digitalWrite(PWR_PIN, HIGH);
  delay(1500);
  digitalWrite(PWR_PIN, LOW);
}

void modemRestart() {
  modemPowerOff();
  delay(1000);
  modemPowerOn();
}

String getLockState() {
  TinyGsmClient client(modem);
  if (!client.connect("194.31.55.182", 3001)) {
    SerialMon.println("Failed to connect to get lock state");
    return "";
  }

  client.println("GET /api/lock HTTP/1.1");
  client.println("Host: 194.31.55.182");
  client.println("Connection: close");
  client.println();

  unsigned long timeout = millis();
  while (client.available() == 0 && millis() - timeout < 5000) {}

  String line;
  while (client.available()) {
    line = client.readStringUntil('\n');
    line.trim();
    if (line.startsWith("{")) break;
  }

  client.stop();

  if (line.indexOf("\"locked\"") != -1) return "locked";
  if (line.indexOf("\"unlocked\"") != -1) return "unlocked";

  SerialMon.println("Failed to parse lock state from:");
  SerialMon.println(line);
  return "";
}

const unsigned long gpsInterval = 50000;     // 50 seconds

void setup() {
  SerialMon.begin(115200);
  delay(10);

  lockServo.attach(SERVO_PIN);
  lockServo.write(90);

  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, HIGH);

  if (!mpu.begin()) {
  SerialMon.println("Failed to find MPU6050 chip");
  while (1) delay(10);
}
  SerialMon.println("MPU6050 found!");

  mpu.setHighPassFilter(MPU6050_HIGHPASS_0_63_HZ);
  mpu.setMotionDetectionThreshold(10);
  mpu.setMotionDetectionDuration(50);
  mpu.setInterruptPinLatch(true);
  mpu.setInterruptPinPolarity(true);
  mpu.setMotionInterrupt(true);


  modemPowerOn();
  SerialAT.begin(UART_BAUD, SERIAL_8N1, PIN_RX, PIN_TX);

  SerialMon.println("Initializing modem...");
  delay(10000);
}

void loop() {
  if (!modem.isGprsConnected()) {
    modem.gprsConnect(apn, gprsUser, gprsPass);
  }

  static unsigned long lastGpsCheck = 0;
  static unsigned long gpsStartTime = 0;
  static int gpsAttempts = 0;
  static bool gpsInProgress = false;
  static bool gpsEnabled = false;
  static float lat, lon;

  unsigned long now = millis();

  // Start GPS fix if it's time
  if (!gpsInProgress && now - lastGpsTime > gpsInterval) {
    enableGPS();
    gpsStartTime = now;
    gpsAttempts = 0;
    gpsInProgress = true;
    gpsEnabled = true;
    SerialMon.println("Starting GPS fix attempt...");
  }

  // If GPS in progress, check every 2s
  if (gpsInProgress && now - lastGpsCheck >= 2000) {
    lastGpsCheck = now;
    gpsAttempts++;

    if (modem.getGPS(&lat, &lon) && lat != 0.0 && lon != 0.0) {
      SerialMon.print("GPS Fix: "); SerialMon.print(lat, 6);
      SerialMon.print(", "); SerialMon.println(lon, 6);

      TinyGsmClient client(modem);
      if (client.connect("194.31.55.182", 3001)) {
        String payload = String("{\"lat\":") + String(lat, 6) + ",\"lon\":" + String(lon, 6) + "}";
        client.println("POST /api/location HTTP/1.1");
        client.println("Host: 194.31.55.182");
        client.println("Content-Type: application/json");
        client.print("Content-Length: ");
        client.println(payload.length());
        client.println();
        client.println(payload);
        SerialMon.println("Location sent to server.");

        while (client.available()) {
          SerialMon.println(client.readStringUntil('\n'));
        }
        client.stop();
      }

      lastGpsTime = now;
      gpsInProgress = false;
      if (gpsEnabled) {
        disableGPS();
        gpsEnabled = false;
      }
    } else {
      SerialMon.println("Waiting for GPS fix...");
    }

    // Timeout after 30 tries (~60 seconds)
    if (gpsAttempts >= 30) {
      SerialMon.println("GPS fix timed out.");
      gpsInProgress = false;
      if (gpsEnabled) {
        disableGPS();
        gpsEnabled = false;
      }
    }
  }

  if (now - lastLockCheck > lockPollInterval) {
    lastLockCheck = now;

    String lockStatus = getLockState();

    if (lockStatus != "" && lockStatus != currentLockState) {
      SerialMon.print("Updating lock to: ");
      SerialMon.println(lockStatus);

      if (lockStatus == "locked") {
        lockServo.write(0);
      } else if (lockStatus == "unlocked") {
        lockServo.write(90);
      }

      currentLockState = lockStatus;
    }
  }

  // Check for movement if locked
if (currentLockState == "locked" && mpu.getMotionInterruptStatus()) {
  SerialMon.println("Aptiktas dviračio judėjimas!");

  // Send alert to server
  TinyGsmClient client(modem);
  if (client.connect("194.31.55.182", 3001)) {
    String payload = String("{\"deviceId\":\"") + deviceId + "\",\"message\":\"Aptiktas dviračio judėjimas!\"}";

    client.println("POST /api/motion-alert HTTP/1.1");
    client.println("Host: 194.31.55.182");
    client.println("Content-Type: application/json");
    client.print("Content-Length: ");
    client.println(payload.length());
    client.println();
    client.println(payload);

    SerialMon.println("Motion alert sent to server.");

    while (client.available()) {
      SerialMon.println(client.readStringUntil('\n'));
    }
    client.stop();
  } else {
    SerialMon.println("Failed to connect for motion alert.");
  }

  delay(5000);  // prevent spamming the server
}


  delay(200); // non-blocking cycle
}



