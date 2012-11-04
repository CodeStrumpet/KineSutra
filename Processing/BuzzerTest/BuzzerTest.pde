import processing.serial.*;

Serial port;

int SKEL_HEAD = 0, 
    SKEL_NECK = 1, 
    SKEL_LEFT_SHOULDER = 2, 
    SKEL_RIGHT_SHOULDER = 3, 
    SKEL_LEFT_ELBOW = 4, 
    SKEL_RIGHT_ELBOW = 5, 
    SKEL_TORSO = 6, 
    SKEL_LEFT_HIP = 7, 
    SKEL_RIGHT_HIP = 8, 
    SKEL_LEFT_KNEE = 9, 
    SKEL_RIGHT_KNEE = 10, 
    SKEL_LEFT_HAND = 11, 
    SKEL_RIGHT_HAND = 12, 
    SKEL_LEFT_FOOT = 13, 
    SKEL_RIGHT_FOOT = 14;

int LEFT = 0, RIGHT = 1;

char[][] buzzerMap = {
  {
    'A', 'B'
  }, {
    'C', 'D'
  }
  
  // Add more buzzer mapping here.
};

void setup() {
  port = new Serial(this, Serial.list()[0], 115200);
}

void draw() {
}

void keyPressed() {
  // Write the buzzer's corresponding letter here.
  port.write('A');
}

