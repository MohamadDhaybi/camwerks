//Serial Command library from https://github.com/scogswell/ArduinoSerialCommand
#include <SerialCommand.h>
#include <SoftwareSerial.h>

SerialCommand SCmd;   // The demo SerialCommand object
#define arduinoLED 13   // Arduino LED on board

boolean DEBUG = false;

// Variables for Callipper Reading
//int i;
//int sign;
//long value;
float measure;
//int clockpin = 9;
//int datapin = 8;
unsigned long tempmicros;
unsigned long revMillis;

int req = 10; //mic REQ line goes to pin 5 through q1 (arduino high pulls request line low)
int dat = 8; //mic Data line goes to pin 2
int clk = 9; //mic Clock line goes to pin 3
int i = 0;
int j = 0;
int k = 0;
int signCh = 8;
int sign = 0;
int decimal;
float dpp;
int units;

byte mydata[14];
String value_str;
long value_int; //was an int, could not measure over 32mm
float value;

//Variables for Stepper Motor control
int dirPin=6;//connected to Arduino's port 6
int stepPin=7;//connected to Arduino's port 7

// Variables for measuring process
int nbCycles = 1;
int nbSteps = 600; // number of steps for a full revolution
int stepCount = 0; // will 0/600
int currentStep = 1; // will loop 1/2/3/4
int stepDirection = -1; // 1 : clockwise / -1 : counter clockwise

void setup() {
  //Calliper Reading
  Serial.begin(9600);
  
  pinMode(req, OUTPUT);
  pinMode(clk, INPUT_PULLUP);
  pinMode(dat, INPUT_PULLUP);
  digitalWrite(req,LOW); // set request at high

  //Stepper Motor
  pinMode(dirPin,OUTPUT);
  pinMode(stepPin,OUTPUT);

  digitalWrite(dirPin,HIGH);//enable motorA
  digitalWrite(stepPin,LOW);//enable motorB

  // Setup callbacks for SerialCommand commands 
  SCmd.addCommand("PING",ping);
  SCmd.addCommand("STEP",doStep); //Do one step 
  SCmd.addCommand("MEASURE",getMeasure); //Get measure and print result 
  SCmd.addCommand("SET_DIRECTION",setDirection); //Set the direction of rotation
  
    SCmd.addDefaultHandler(unrecognized);          // Handler for command that isn't matched  (says "What?") 
}

void loop()
{  
  SCmd.readSerial();     // We don't do much, just process serial commands
}

void getMeasure(){
      if(!DEBUG){
        readCalliper();
      }
      else{ //DEBUG VERSION
        measure = random(0, 10);
      }

      //Serial.print("MEASURE ");
      //measure = -12.10;
      //measure = value;
      Serial.println(measure,2);
}

void doStep() {
  digitalWrite(dirPin,stepDirection);
  digitalWrite(stepPin,HIGH);
  delayMicroseconds(500);
  digitalWrite(stepPin,LOW);
  
  Serial.println("STEP_ACK"); 
}

void readCalliper() {
  digitalWrite(req, HIGH); // generate set request
for( i = 0; i < 13; i++ ) {
k = 0;
for (j = 0; j < 4; j++) {
while( digitalRead(clk) == LOW) {
} // hold until clock is high
while( digitalRead(clk) == HIGH) {
} // hold until clock is low
bitWrite(k, j, (digitalRead(dat) & 0x1));
}

mydata[i] = k;

}
sign = mydata[4];
value_str = String(mydata[5]) + String(mydata[6]) + String(mydata[7]) + String(mydata[8] + String(mydata[9] + String(mydata[10]))) ;
decimal = mydata[11];
units = mydata[12];

value_int = value_str.toInt();
if (decimal == 0) dpp = 1.00;
if (decimal == 1) dpp = 10.00;
if (decimal == 2) dpp = 100.00;
if (decimal == 3) dpp = 1000.00;
if (decimal == 4) dpp = 10000.00;
if (decimal == 5) dpp = 100000.00;

value = value_int / dpp;
measure = sign==0 ? value:-value;
digitalWrite(req,LOW);
}

void decodeCalliper() {
  measure=value;
}

void ping()
{
  Serial.println("PING_ACK"); 
}

void setDirection() {
  String arg = SCmd.next();
  if (arg != NULL) {
    stepDirection = arg.toInt();
  //if(stepDirection < 0)
  //  stepDirection = 1;
  //else
  //  stepDirection = 0;
  stepDirection = stepDirection < 0 ? 1:0;
  } 
  Serial.println("DIR_ACK"); 
}


// This gets set as the default handler, and gets called when no other command matches. 
void unrecognized()
{
  Serial.println("What?"); 
}
