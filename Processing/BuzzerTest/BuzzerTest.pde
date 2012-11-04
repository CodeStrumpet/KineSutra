import java.util.Arrays;
import processing.serial.*;

Serial port;

final int THRESHOLD = 1;

Character SKEL_HEAD = 0, 
    SKEL_NECK = 1, 
    SKEL_LEFT_SHOULDER = 2, 
    SKEL_LEFT_ELBOW = 3, 
    SKEL_LEFT_HAND = 4, 
    SKEL_RIGHT_SHOULDER = 5, 
    SKEL_RIGHT_ELBOW = 6, 
    SKEL_RIGHT_HAND = 7, 
    SKEL_TORSO = 8, 
    SKEL_LEFT_HIP = 9, 
    SKEL_LEFT_FOOT = 10, 
    SKEL_LEFT_KNEE = 11, 
    SKEL_RIGHT_HIP = 12, 
    SKEL_RIGHT_KNEE = 13, 
    SKEL_RIGHT_FOOT = 14;
    
Map<List<Character>, Character> buzzerMap;
    
void setup() {
  port = new Serial(this, "/dev/tty.FireFly-5F27-SPP", 115200);
  
  // Buzz mappings (joint, coordinate, direction) => Buzzer code
  buzzerMap = new HashMap<List<Character>, Character>();
  buzzerMap.put(Arrays.asList(SKEL_RIGHT_FOOT, 'x', '+'), 'A');
  buzzerMap.put(Arrays.asList(SKEL_RIGHT_FOOT, 'x', '-'), 'a');
  buzzerMap.put(Arrays.asList(SKEL_RIGHT_FOOT, 'y', '+'), 'B');
  buzzerMap.put(Arrays.asList(SKEL_RIGHT_FOOT, 'y', '-'), 'b');
}

void draw() {
}

void keyPressed() {
  // Write the buzzer's corresponding letter here.
  directMoves(SKEL_RIGHT_FOOT, 1, 1, 0);
}

/**
 * Directs a joint move to a buzzer.
 *
 * Args:
 *   joint - Joint number
 *   mx    - Movement on the x-axis
 *   my    - Movement on the y-axis
 *   mz    - Movement on the z-axis
 */
void directMoves(char joint, int mx, int my, int mz) {
  directMove(joint, 'x', mx);
  directMove(joint, 'y', my);
  directMove(joint, 'z', mz);
}

void directMove(char joint, char coordinate, int value) {
 if (abs(value) >= THRESHOLD) {
    // Above the threshold limit. Something's gonna buzz!
    char direction = (value >= THRESHOLD) ? '+' : '-';
    println(buzzerMap.get(Arrays.asList(joint, coordinate, direction)));
    port.write(buzzerMap.get(Arrays.asList(joint, coordinate, direction)));
  } 
}
