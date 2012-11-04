#define PCUART Serial
#define PCBAUD 115200
#define NUM_BUZZERS 3

const int pins[NUM_BUZZERS] = {13, 14, 15};
const int buzz_duration = 100; //in ms

int ledpin = 12;

unsigned long timers[NUM_BUZZERS];

char buzzer;

void setup() {  
  pinMode(ledpin, OUTPUT);
  
  PCUART.begin(PCBAUD);  
   
   for (int i=0;i<NUM_BUZZERS;i++) {
     pinMode(pins[i],OUTPUT);
     digitalWrite(pins[i],LOW);
     timers[i] = millis();
   }
     
}

void loop()
{
  
  while (Serial.available() > 0) {
    buzzer = (char)Serial.read() - 65;  //"A" will be a 0, B=1, etc...
    timers[buzzer] = millis() + buzz_duration;
    digitalWrite(pins[buzzer],HIGH);
    digitalWrite(ledpin, HIGH);

  }
   
   for (int i=0;i<NUM_BUZZERS;i++) {
     if (timers[i]<millis()) //this buzzer's time is up, so turn it off
       digitalWrite(pins[i],LOW);
      digitalWrite(ledpin, LOW);

   }
      
}
