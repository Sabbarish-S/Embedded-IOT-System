#include <WiFi.h>
#include <FirebaseESP32.h>
#include <DHT.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BMP085_U.h>


#define WIFI_SSID "Saran" //"TATA WIFI_2.4GHz"  //Dell2015//
#define WIFI_PASSWORD "12345678"//"ssss@2003"     //"vishwa0909009"//

#define DHTPIN 4       // Pin where DHT11 is connected
#define DHTTYPE DHT11  // DHT11 sensor
DHT dht(DHTPIN, DHTTYPE);

#define RAIN_SENSOR_AO 34  // Analog pin connected to AO of the rain sensor
#define RAIN_SENSOR_DO 35  // Digital pin connected to DO of the rain sensor

Adafruit_BMP085_Unified bmp = Adafruit_BMP085_Unified(10085);

FirebaseData firebaseData;
FirebaseConfig config;

// Define thresholds for rain detection
#define NO_RAIN_THRESHOLD 4095    // No rain
#define LIGHT_RAIN_THRESHOLD 3000  // Light rain
#define MODERATE_RAIN_THRESHOLD 2000 // Moderate rain
#define HEAVY_RAIN_THRESHOLD 1000   // Heavy rain




void setup() {
  Serial.begin(115200);
  dht.begin();
  
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }
  Serial.println("Connected to WiFi");
  config.host = "tarp-7b4b0-default-rtdb.firebaseio.com";
  config.signer.tokens.legacy_token = "j4FEiLeVxN8WnFaQFo5r7AT7amPRCH0OfbYPV5ZW";

  Firebase.begin(&config, NULL);  // Pass the config and no auth
  Firebase.reconnectWiFi(true);

  // Check Firebase connection status
  if (Firebase.ready()) {
    Serial.println("Firebase is ready!");
  } else {
    Serial.println("Firebase connection failed!");
  }

  pinMode(RAIN_SENSOR_DO, INPUT);

  if (!bmp.begin()) {
    Serial.println("BMP180 not detected.");
    while (1);
  }
}

void loop() {
  float temp = dht.readTemperature();
  float humidity = dht.readHumidity();
  int rainAnalogValue = analogRead(RAIN_SENSOR_AO);
  Serial.print("Raw Analog Rain Sensor Value: ");
  Serial.println(rainAnalogValue);
  
  int rainDigitalValue = digitalRead(RAIN_SENSOR_DO);
  String rainStatus = (rainDigitalValue == LOW) ? "Rain detected" : "No rain detected";
  Serial.println(rainStatus);

  // Determine rain status based on thresholds
  String detailedRainStatus;
  if (rainAnalogValue >= NO_RAIN_THRESHOLD) {
    detailedRainStatus = "No rain";
  } else if (rainAnalogValue >= LIGHT_RAIN_THRESHOLD) {
    detailedRainStatus = "Light rain detected";
  } else if (rainAnalogValue >= MODERATE_RAIN_THRESHOLD) {
    detailedRainStatus = "Moderate rain detected";
  } else if (rainAnalogValue >= HEAVY_RAIN_THRESHOLD) {
    detailedRainStatus = "Heavy rain detected";
  } else {
    detailedRainStatus = "Sensor fully wet";
  }

  // BMP180 sensor readings
  sensors_event_t event;
  bmp.getEvent(&event);
  
  if (event.pressure) {
    float bmpTemperature;
    bmp.getTemperature(&bmpTemperature);

    float pressure = event.pressure;
    float altitude = bmp.pressureToAltitude(SENSORS_PRESSURE_SEALEVELHPA, event.pressure);

    // Log BMP180 sensor values
    // Serial.print("BMP180 Temperature: ");
    // Serial.print(bmpTemperature);
    // Serial.println(" C");

    Serial.print("BMP180 Pressure: ");
    Serial.print(pressure);
    Serial.println(" hPa");

    Serial.print("BMP180 Altitude: ");
    Serial.print(altitude);
    Serial.println(" meters");

    // Send BMP180 data to Firebase
    // if (Firebase.setFloat(firebaseData, "/sensors/bmp_temperature", bmpTemperature)) {
    //   Serial.println("BMP180 Temperature sent to Firebase: " + String(bmpTemperature));
    // }
    if (Firebase.setFloat(firebaseData, "/sensors/pressure", pressure)) {
      Serial.println("BMP180 Pressure sent to Firebase: " + String(pressure));
    }
    if (Firebase.setFloat(firebaseData, "/sensors/altitude", altitude)) {
      Serial.println("BMP180 Altitude sent to Firebase: " + String(altitude));
    }
  }

  // Send rain data to Firebase
  if (Firebase.setInt(firebaseData, "/sensors/rain_analog", rainAnalogValue)) {
    Serial.println("Analog rain value sent to Firebase: " + String(rainAnalogValue));
  }

  if (Firebase.setString(firebaseData, "/sensors/rain_status", detailedRainStatus)) {
    Serial.println("Detailed rain status sent to Firebase: " + detailedRainStatus);
  }
//DHT logic  
  if (!isnan(temp) && !isnan(humidity)) {
    if (Firebase.setFloat(firebaseData, "/sensors/temperature", temp)) {
      Serial.println("Temperature sent: " + String(temp));
    }
    if (Firebase.setFloat(firebaseData, "/sensors/humidity", humidity)) {
      Serial.println("Humidity sent: " + String(humidity));
    }
  } else {
    Serial.println("Failed to read from DHT sensor!");
  }

  

  delay(1000);  // Delay for 2 seconds before next read
}
