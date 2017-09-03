
#include <Wire.h>
#include <SFE_MMA8452Q.h>
MMA8452Q accel;

// steps and sleeps initialization //
int countSteps = 0;
int countSleep = 0;
int peak = 0;
unsigned long sleepTimer = 0;
double ax = 0; double ay = 0; double az = 0;
double storeZ_steps[] = {-1000.0,-1000.0};
double storeXYZ_sleep[] = {-1000.0,-1000.0,-1000.0};

// state switch initialization //
int stateButton = 12;
int stateLED = 13;

// reset button //
int resetButton = 11;

// delta times //
unsigned long stepsTime = 0;
unsigned long sleepTime = 0;
float stepSpeed = 0;
unsigned long stepStart = 0;

enum State {
  trackSleep,
  trackSteps
};

State fitbitState = trackSleep;

void setup() {
  Serial.begin(9600);
  Serial.println("Test begins");

  pinMode(stateButton, INPUT_PULLUP);
  pinMode(resetButton, INPUT_PULLUP);
  pinMode(stateLED, OUTPUT);
  accel.init();
}

unsigned long runTimer = 0;

void loop() {
    fitbitState = nextState(fitbitState);
    switchStateMethod();
    reset();

    unsigned long now = millis();
    if (now - runTimer >= 1000) {
      if (fitbitState == trackSleep) {
        sendSleep();
        if (sleepTimer < 0) sendError();
      }
      else if (fitbitState == trackSteps) {
        sendSteps();
        sendSpeed();
        if (peak < 0) sendError();
      }

      sendTemp();
      sendRunTime(now);
      runTimer += 1000;
    }
}

unsigned long switchTimer = 0;
unsigned long resetTimer = 0;
bool switchState = LOW;
bool switchLastState = LOW;
bool resetState = LOW;
bool resetLastState = LOW;


void switchStateMethod() {
  bool readBT = digitalRead(stateButton);

  if (millis() - switchTimer >= 50) {
    if (readBT != switchState) {
      switchState = readBT;
      if (!switchState) {
        if (fitbitState == trackSleep) {
          fitbitState = trackSteps;
          countSteps = 0;
          stepsTime = millis();
          peak = 0;
        }
        else if (fitbitState == trackSteps) {
          fitbitState = trackSleep;
          countSleep = 0;
          sleepTime = millis();
          sleepTimer = 0;
        }
      }
    }
  }
  switchLastState = readBT;
}

void reset() {
  bool readBT = digitalRead(resetButton);

  if (millis() - resetTimer >= 50) {
    if (readBT != resetState) {
      resetState = readBT;
      if (!resetState) {
        if (fitbitState == trackSteps) {
          countSteps = 0;
          stepsTime = millis();
          stepStart = millis();
          peak = 0;
        }
        else if (fitbitState == trackSleep) {
          countSleep = 0;
          sleepTime = millis();
          sleepTimer = 0;
        }
      }
    }
  }
  switchLastState = readBT;
}

void printCalculatedAccels() {
  countSteps++;
  countSleep++;

  ax = accel.cx;
  ay = accel.cy;
  az = accel.cz;
}

void trackCoords() {
  if (fitbitState == trackSteps) {
    if (countSteps % 2 == 1) {
      storeZ_steps[0] = az;
    }
    else {
      storeZ_steps[1] = az;
    }
  }
  else if (fitbitState == trackSleep) {
    storeXYZ_sleep[0] = ax;
    storeXYZ_sleep[1] = ay;
    storeXYZ_sleep[2] = az;
  }
}

bool preventFallsAsPeaks = false;

State nextState(State state) {
  switch (state) {
    case trackSleep:
    if (millis() - sleepTime >= 50) {
      if (accel.available()) {
        digitalWrite(stateLED, HIGH);
        accel.read();
        printCalculatedAccels();

        if (countSleep > 0) {
          if (abs(ax-storeXYZ_sleep[0]) < 0.1 || abs(ay-storeXYZ_sleep[1]) < 0.1 || abs(az-storeXYZ_sleep[2]) < 0.1) {
            sleepTimer+=50;
         }
         else {
          Serial.println("Not at rest");
         }
        }
      trackCoords();
      sleepTime += 50;
      }
    }
    
    
    break;

    case trackSteps:

    if (millis() - stepsTime >= 50) {
      if (accel.available()) {;
        digitalWrite(stateLED, LOW);
        accel.read();
        printCalculatedAccels();

        sendRawAccel(accel.cz);

        if (az < 0.7) preventFallsAsPeaks = true;

      if (preventFallsAsPeaks) {
        if (countSteps > 2) {
          if (storeZ_steps[0] > -1000 && storeZ_steps[1] > -1000) {
            if ((az - storeZ_steps[0] > 0.2) && (az - storeZ_steps[1] > 0.2) && az > 1.2) {
              peak+=1;
              Serial.println("This is a peak");
              preventFallsAsPeaks = false;
            }
            }
          }
        }
        trackCoords();
        stepsTime += 50;
      }
    }
    
    break;
  }
  return state;
}

void sendDebug(String s) {
  Serial.write(0x40);
  Serial.write(0x30);
  Serial.write(s.length() >> 8);
  Serial.write(s.length());

  for (int i = 0; i < s.length(); i++) {
    Serial.write(s[i]);
  }
}

void sendError() {

  String s = "High Alarm";
  
  Serial.write(0x40);
  Serial.write(0x31);
  Serial.write(s.length() >> 8);
  Serial.write(s.length());

  for (int i = 0; i < s.length(); i++) {
    Serial.write(s[i]);
  }
}

int tempScale = 0;

void sendTemp() {
  Serial.write(0x40);
  Serial.write(0x32);

  int tempRead = analogRead(tempScale);
  float temperature = 25 + (tempRead*(5.0/1023)-0.75)*100;
  unsigned long longTemperature = *(unsigned long *) &temperature;

  Serial.write(longTemperature >> 24);
  Serial.write(longTemperature >> 16);
  Serial.write(longTemperature >> 8);
  Serial.write(longTemperature);
  
}

void sendSteps() {
  Serial.write(0x40);
  Serial.write(0x33);
  Serial.write(peak >> 8);
  Serial.write(peak);

}

void sendSleep() {
  Serial.write(0x40);
  Serial.write(0x34);
  Serial.write(sleepTimer >> 24);
  Serial.write(sleepTimer >> 16);
  Serial.write(sleepTimer >> 8);
  Serial.write(sleepTimer);

}

void sendRunTime(unsigned long now) {
  Serial.write(0x40);
  Serial.write(0x35);
  Serial.write(now >> 24);
  Serial.write(now >> 16);
  Serial.write(now >> 8);
  Serial.write(now);
}

void sendRawAccel(float f) {
  unsigned long longF = *(unsigned long *) &f;
  
  Serial.write(0x40);
  Serial.write(0x36);
  Serial.write(longF >> 24);
  Serial.write(longF >> 16);
  Serial.write(longF >> 8);
  Serial.write(longF);
}

void sendSpeed() {
  unsigned long walkedTime = millis() - stepStart;
  stepSpeed = (peak * 1.0 / walkedTime) * 3600000; 
  unsigned long longSpeed = *(unsigned long *) &stepSpeed;

  Serial.write(0x40);
  Serial.write(0x37);
  Serial.write(longSpeed >> 24);
  Serial.write(longSpeed >> 16);
  Serial.write(longSpeed >> 8);
  Serial.write(longSpeed);
}


