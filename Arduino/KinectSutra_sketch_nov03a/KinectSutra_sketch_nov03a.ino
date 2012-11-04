#define PCUART Serial
#define PCBAUD 115200
#define NUM_BUZZERS 24

const int pins[NUM_BUZZERS] = {30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53};
const int buzz_duration = 500; //in ms

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
    if (buzzer>=0 && buzzer<NUM_BUZZERS) {
    timers[buzzer] = millis() + buzz_duration;
    digitalWrite(pins[buzzer],HIGH);
    digitalWrite(ledpin, HIGH);
    }

  }
   
   for (int i=0;i<NUM_BUZZERS;i++) {
     if (timers[i]<millis()) { //this buzzer's time is up, so turn it off
       digitalWrite(pins[i],LOW);
      digitalWrite(ledpin, LOW);
     }

   }
      
}
