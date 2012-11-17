// KineSutra processing
//
// November 3, 2012
//
// Paul Mans
// Brent Townshend
// Islam El-Ashi

import SimpleOpenNI.*;
import controlP5.*;
import processing.serial.*;

public class KineSutra extends PApplet {
// This code need serious refactoring to make it less spaghetti
// Should have separate class for all the joint position arrays
// Should use vector class underneath for math operations on the joint positions

SimpleOpenNI  kinect;
PrintWriter logger;
Serial port;

int NUM_JOINTS = 15;

int [] jointIDs;
Boolean referenceJointsAreSet = false;  // Once we've acquired a reference
Boolean bluetooth = false;
Boolean serial=true;

// Various vectors of joint positions
PVector currentJointPositions[] = new PVector[NUM_JOINTS];
PVector referenceJointPositions[] = new PVector[NUM_JOINTS];
PVector movementVectors[] = new PVector[NUM_JOINTS];
PVector targetVectors[] = new PVector[NUM_JOINTS];

class Pose {
    PVector[] positions;
    PImage depthImage;
    PImage rgbImage;

    void Set(PVector[] p) { for (int j=0;j<NUM_JOINTS;j++) positions[j]=p[j]; }
};

// Which joint is upstream of this joint (or -1 if none)
int[] priorJoint = {1,8,8,2,3,8,5,6,-1,8,9,10,8,12,13};

Map<List<Character>, Character> buzzerMap;   // Mapping from joint moves to buzzer commands
int threshold = 200;  // Threshold for requiring movement (mm)

// Joint indexes (different from SimpleOpenNI numbering)
char SKEL_HEAD = 0, 
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
String jointNames[]={"Head","Neck","Left Shoulder","Left Elbow","Left Hand","Right Shoulder","Right Elbow","Right Hand","Torso","Left Hip","Left Foot","Left Knee","Right Hip","Right Knee","Right Foot"};

int sample=0;   // Current sample number (primarily for logging)
int sday,shour,smin,ssec,smillis;   // Start time of program
float hapticUpdateInterval = 1.0;   // Update haptics at this interval in seconds
float lastHapticUpdate;	   // Elapsed time since start of last haptic update
int updatingJoint=-1;	   // Which joint is currently being updated
PImage refImgDepth,refImgRGB;
boolean useDepth=false;
ControlP5 cp5;

void setup() {
    if (bluetooth)
	port = new Serial(this, "/dev/tty.FireFly-5F27-SPP", 115200);
    else if (serial) {
	// List all the available serial ports:
	println(Serial.list());

	// Open the port you are using at the rate you want:
	port = new Serial(this, Serial.list()[12], 115200);
    }
    frameRate(30);
    size(1024, 768);
    kinect = new SimpleOpenNI(this);
    kinect.enableDepth();
    kinect.enableRGB();
    kinect.enableUser(SimpleOpenNI.SKEL_PROFILE_ALL);
    kinect.setMirror(true);
    cp5=new ControlP5(this);
    cp5.addSlider("threshold").setPosition(20,500).setSize(40,200).setRange(10,500).setValue(200);
    cp5.addToggle("useDepth").setPosition(100,680).setSize(50,20) ;
    logger=createWriter("poses"+year()+"_"+month()+"_"+day()+"_"+hour()+"_"+minute()+".txt");

    // Would've been nice to initialize this above, but causes link errors to reference these before calling the SimpleOpenNI constructor or other methods
    // Initialize a temporary
    int [] joints = new int[]{SimpleOpenNI.SKEL_HEAD, 
			      SimpleOpenNI.SKEL_NECK, 
			      SimpleOpenNI.SKEL_LEFT_SHOULDER,
			      SimpleOpenNI.SKEL_LEFT_ELBOW,
			      SimpleOpenNI.SKEL_LEFT_HAND,
			      SimpleOpenNI.SKEL_RIGHT_SHOULDER,
			      SimpleOpenNI.SKEL_RIGHT_ELBOW,
			      SimpleOpenNI.SKEL_RIGHT_HAND,
			      SimpleOpenNI.SKEL_TORSO,
			      SimpleOpenNI.SKEL_LEFT_HIP,
			      SimpleOpenNI.SKEL_LEFT_KNEE,
			      SimpleOpenNI.SKEL_LEFT_FOOT,
			      SimpleOpenNI.SKEL_RIGHT_HIP,
			      SimpleOpenNI.SKEL_RIGHT_KNEE,
			      SimpleOpenNI.SKEL_RIGHT_FOOT};
    // Copy into global
    jointIDs=joints;

    setBuzzerMappings();

    // Start time reference
    sday=day();
    shour=hour();
    smin=minute();
    ssec=second();
    smillis=millis();


    for (int joint=0;joint<NUM_JOINTS;joint++) {
	currentJointPositions[joint]=new PVector();
	referenceJointPositions[joint]=new PVector();
	movementVectors[joint]=new PVector();
	targetVectors[joint]=new PVector();
    }
    if (bluetooth)
	BuzzAll();
}

// Elapsed time in seconds since startup
// Will roll over after 24 hours of continuous running
float elapsed() {
      return ((((day()-sday)*24+(hour()-shour))*60)+(minute()-smin))*60+(second()-ssec)+(millis()-smillis)/1000.0;
}

int currentUser;

// Processing main draw loop
void draw() {
    background(0);
    kinect.update();
    if (useDepth)
	image(kinect.depthImage(), 0,0);
    else
	image(kinect.rgbImage(), 0,0);
    
     currentUser = -1;
    
    IntVector userList = new IntVector();
    kinect.getUsers(userList);
    if (userList.size() > 0) {
        int userId = userList.get(0);
        if( kinect.isTrackingSkeleton(userId)) {
            currentUser = userId;
        }
    }

    if (referenceJointsAreSet) {
	pushMatrix();
	if (useDepth)
	    image(refImgDepth,640,0,320,240);
	else
	    image(refImgRGB,640,0,320,240);
	translate(640,240);
	scale(0.5);
	stroke(0,255,0);
	strokeWeight(2);
	drawLimbs(referenceJointPositions);
	popMatrix();
    }
    // draw the skeleton in whatever color we chose
    if (currentUser > 0) {
	// Load current joints from Kinect
	for (int joint = 0; joint < jointIDs.length; joint++) {
	    PVector jointVector = new PVector();

	    kinect.getJointPositionSkeleton(currentUser, jointIDs[joint], jointVector);
        
	    currentJointPositions[joint].x = jointVector.x;
	    currentJointPositions[joint].y = jointVector.y;
	    currentJointPositions[joint].z = jointVector.z;
	}

        if (shouldSendCurrentMovementVectors()) {
	    processSkeletonFromCurrentFrame(currentUser);
            sendCurrentMovementVectors();
        }
        pushMatrix();
	translate(0,0);
	drawSkeleton();        
	popMatrix();
    } 
    
    text("Set reference pose by pressing 'r'", 40, height - 40);

    if (updatingJoint == -1 && referenceJointsAreSet)  {
	text("ON TARGET",200,620);
	fill(0,255,0);
	ellipse(512,600,100,100);
    }
}

// Draw entire skeleton
// Current position in red,  target position in green
void drawSkeleton() {
    strokeWeight(5);
    stroke(0,0,255);
    // For comparision
    kinect.drawLimb(currentUser, SimpleOpenNI.SKEL_HEAD, SimpleOpenNI.SKEL_NECK);

    stroke(255,0,0);
    drawLimbs(currentJointPositions);
    if (referenceJointsAreSet) {
	stroke(0,255,0);
	drawLimbs(targetVectors);
    }
}

// Draw all limbs using joint points in p
void drawLimbs(PVector p[]) {
    drawLimb(p,SKEL_HEAD, SKEL_NECK);
    drawLimb(p,SKEL_NECK, SKEL_LEFT_SHOULDER);
    drawLimb(p,SKEL_LEFT_SHOULDER, SKEL_LEFT_ELBOW);
    drawLimb(p,SKEL_LEFT_ELBOW, SKEL_LEFT_HAND);
    drawLimb(p,SKEL_NECK, SKEL_RIGHT_SHOULDER);
    drawLimb(p,SKEL_RIGHT_SHOULDER, SKEL_RIGHT_ELBOW);
    drawLimb(p,SKEL_RIGHT_ELBOW, SKEL_RIGHT_HAND);
    drawLimb(p,SKEL_LEFT_SHOULDER, SKEL_TORSO);
    drawLimb(p,SKEL_RIGHT_SHOULDER, SKEL_TORSO);
    drawLimb(p,SKEL_TORSO, SKEL_LEFT_HIP);
    drawLimb(p,SKEL_LEFT_HIP, SKEL_LEFT_KNEE);
    drawLimb(p,SKEL_LEFT_KNEE, SKEL_LEFT_FOOT);
    drawLimb(p,SKEL_TORSO, SKEL_RIGHT_HIP);
    drawLimb(p,SKEL_RIGHT_HIP, SKEL_RIGHT_KNEE);
    drawLimb(p,SKEL_RIGHT_KNEE, SKEL_RIGHT_FOOT);
    drawLimb(p,SKEL_RIGHT_HIP, SKEL_LEFT_HIP);
}

// Draw limb using joint points in p
void drawLimb(PVector p[],int joint1, int joint2) {
     PVector p1world=new PVector(),p2world=new PVector();
     PVector p1proj=new PVector(), p2proj=new PVector();
     p1world.x=p[joint1].x;
     p1world.y=p[joint1].y;
     p1world.z=p[joint1].z;
     p2world.x=p[joint2].x;
     p2world.y=p[joint2].y;
     p2world.z=p[joint2].z;
     
     kinect.convertRealWorldToProjective(p1world,p1proj);
     kinect.convertRealWorldToProjective(p2world,p2proj);
     line(p1proj.x,p1proj.y,p2proj.x,p2proj.y);
     if (updatingJoint != -1 && joint1==updatingJoint)
	 ellipse(p1proj.x,p1proj.y,20,20);
     else if (updatingJoint != -1 && joint2==updatingJoint)
	 ellipse(p2proj.x,p2proj.y,20,20);
}


void log(String s) {
    logger.println(elapsed()+","+s);
}

// Process current skeleton position to determine movements needed
void processSkeletonFromCurrentFrame(int userId) {

    // Process joints
    for (int joint = 0; joint < jointIDs.length; joint++) {
	if (priorJoint[joint]==-1)
	    // This joint is not relative to any other joints, set movement to 0
	    movementVectors[joint].set(0,0,0);
	else {
	    // Relative joint
	    // Movement is amount to make this joint's position relative to its 'priorJoint' (the one it is relative to) equal to the same relationship in the reference
	    // Also, scale limb lengths in case the reference had different length limbs
	    PVector relative=new PVector();
	    PVector refrelative=new PVector();
	    relative = PVector.sub(currentJointPositions[joint],currentJointPositions[priorJoint[joint]]);
	    refrelative = PVector.sub(referenceJointPositions[joint],referenceJointPositions[priorJoint[joint]]);
	    float llen=relative.mag();
	    float refllen=refrelative.mag();

	    if (joint==1)
		println("llen="+llen+",reflen="+refllen);  // Debugging - was seeing some bad lengths

	    float refscale=llen/refllen;   //  Amount to scale reference limb to match limb lengths;
	    // Sanity check -- otherwise can end up with some very bad movements
	    if (refscale<0.8 || refscale >1.3) {
		println("Not scaling reference limb "+joint+","+priorJoint[joint]+" by out of range value " + refscale);
		refscale=1.0;
	    }
	    
	    movementVectors[joint] = PVector.sub(PVector.mult(refrelative,refscale),relative);
	}

	// Compute target positions from movement vectors
	targetVectors[joint]=PVector.add(currentJointPositions[joint],movementVectors[joint]);

	// Log to data file for post-analysis
	log(sample+","+joint + "," + currentJointPositions[joint].x+","+ currentJointPositions[joint].y+","+ currentJointPositions[joint].z+","
	    + referenceJointPositions[joint].x+","+ referenceJointPositions[joint].y+","+ referenceJointPositions[joint].z+","
	    + movementVectors[joint].x+","+ movementVectors[joint].y+","+ movementVectors[joint].z);
    }            
    
    logger.flush();
    sample=sample+1;
}

// Decide if haptic updates should be sent yet
Boolean shouldSendCurrentMovementVectors() {
    float now=elapsed();
    if ((now<0 || (now-lastHapticUpdate) > hapticUpdateInterval) && referenceJointsAreSet) {
	lastHapticUpdate=now;
	return true;
    }
    return false;
}

// Send current movement vectors to buzzers (one at a time)
void sendCurrentMovementVectors() {
    if (updatingJoint==-1 || movementVectors[updatingJoint].mag()<threshold) {
	// Decide which joint should be moving (starting at top and working down a limb at a time)
	// Keep buzzing the same joint until it is within threshold, then go onto the next one that is wrong
	updatingJoint=-1;
	for (int joint=0;joint<jointIDs.length;joint++)
	    if (movementVectors[joint].mag() > threshold) {
		updatingJoint = joint;
		break;
	    }
    }
    if (updatingJoint!=-1) {
	// Buzz the current joint
	println("Move "+jointNames[updatingJoint]+" by "+movementVectors[updatingJoint].x+","+movementVectors[updatingJoint].y+","+movementVectors[updatingJoint].z);
	buzzMoves(updatingJoint,movementVectors[updatingJoint].x,movementVectors[updatingJoint].y,movementVectors[updatingJoint].z);
    }
}

// Grab current positions as reference
void setReferenceJointPositions(int userId) {
    for (int joint = 0; joint < jointIDs.length; joint++) {
	PVector jointVector = new PVector();

	kinect.getJointPositionSkeleton(userId, jointIDs[joint], jointVector);
	referenceJointPositions[joint].x = jointVector.x;
	referenceJointPositions[joint].y = jointVector.y;
	referenceJointPositions[joint].z = jointVector.z;        

	println("Joint "+ joint + "  x: " + jointVector.x + "  y: " + jointVector.y + "  z: " + jointVector.z);
    }
    referenceJointsAreSet = true;
}


void keyPressed(){
    if (key == 's') {
	saveFrame("capture_"+random(100)+".png");      
    }

    if (key == 'r') {
	IntVector userList = new IntVector();
	kinect.getUsers(userList);
	if (userList.size() > 0 &&  kinect.isTrackingSkeleton(userList.get(0))) {
	    println("Acquiring reference");
	    setReferenceJointPositions(userList.get(0));              
	    refImgDepth=createImage(640,480,ALPHA);
	    refImgDepth.loadPixels();
	    refImgDepth.pixels=kinect.depthImage().pixels;
	    refImgDepth.updatePixels();
	    refImgRGB=createImage(640,480,RGB);
	    refImgRGB.loadPixels();
	    refImgRGB.pixels=kinect.rgbImage().pixels;
	    refImgRGB.updatePixels();
	    println("Reference acquired");
	} else {
	    println("Not tracking user - no reference acquired");
	}      
    }
}

// user-tracking callbacks!
void onNewUser(int userId) {
    println("New user: start pose detection");
    kinect.startPoseDetection("Psi", userId);
}

void onEndCalibration(int userId, boolean successful) {
    if (successful) { 
	println("  User calibrated !!!");
	kinect.startTrackingSkeleton(userId);
    } 
    else { 
	println("  Failed to calibrate user !!!");
	kinect.startPoseDetection("Psi", userId);
    }
}

void onStartPose(String pose, int userId) {
    println("Started pose for user");
    kinect.stopPoseDetection(userId); 
    kinect.requestCalibrationSkeleton(userId, true);
}

void setBuzzerMappings() {
    // Buzz mappings (joint, coordinate, direction) => Buzzer code
    buzzerMap = new HashMap<List<Character>, Character>();
    
    buzzerMap.put(Arrays.asList(SKEL_LEFT_SHOULDER, 'x', '+'), 'A');
    buzzerMap.put(Arrays.asList(SKEL_LEFT_SHOULDER, 'x', '-'), 'B');
    
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_SHOULDER, 'x', '+'), 'C');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_SHOULDER, 'x', '-'), 'D');
    
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_HAND, 'x', '+'), 'E');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_HAND, 'x', '-'), 'F');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_HAND, 'y', '+'), 'G');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_HAND, 'y', '-'), 'H');
    
    buzzerMap.put(Arrays.asList(SKEL_LEFT_HAND, 'x', '+'), 'R');
    buzzerMap.put(Arrays.asList(SKEL_LEFT_HAND, 'x', '-'), 'Q');
    buzzerMap.put(Arrays.asList(SKEL_LEFT_HAND, 'y', '+'), 'R');
    buzzerMap.put(Arrays.asList(SKEL_LEFT_HAND, 'y', '-'), 'Q');
    
    buzzerMap.put(Arrays.asList(SKEL_LEFT_ELBOW, 'x', '+'), 'H');
    buzzerMap.put(Arrays.asList(SKEL_LEFT_ELBOW, 'x', '-'), 'G');
    buzzerMap.put(Arrays.asList(SKEL_LEFT_ELBOW, 'y', '+'), 'H');
    buzzerMap.put(Arrays.asList(SKEL_LEFT_ELBOW, 'y', '-'), 'G');
    
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_ELBOW, 'x', '+'), 'I');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_ELBOW, 'x', '-'), 'J');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_ELBOW, 'y', '+'), 'I');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_ELBOW, 'y', '-'), 'J');
    
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_ELBOW, 'x', '+'), 'a');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_ELBOW, 'x', '-'), 'a');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_ELBOW, 'y', '+'), 'B');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_ELBOW, 'y', '-'), 'B');
    
    buzzerMap.put(Arrays.asList(SKEL_LEFT_HIP, 'x', '+'), 'a');
    buzzerMap.put(Arrays.asList(SKEL_LEFT_HIP, 'x', '-'), 'a');
    
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_HIP, 'x', '+'), 'B');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_HIP, 'x', '-'), 'B');
    
    buzzerMap.put(Arrays.asList(SKEL_LEFT_KNEE, 'x', '+'), 'O');
    buzzerMap.put(Arrays.asList(SKEL_LEFT_KNEE, 'x', '-'), 'P');
    buzzerMap.put(Arrays.asList(SKEL_LEFT_KNEE, 'y', '+'), 'O');
    buzzerMap.put(Arrays.asList(SKEL_LEFT_KNEE, 'y', '-'), 'P');
    
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_KNEE, 'x', '+'), 'M');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_KNEE, 'x', '-'), 'K');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_KNEE, 'y', '+'), 'M');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_KNEE, 'y', '-'), 'K');
    
    buzzerMap.put(Arrays.asList(SKEL_LEFT_FOOT, 'x', '+'), 'T');
    buzzerMap.put(Arrays.asList(SKEL_LEFT_FOOT, 'x', '-'), 'T');
    buzzerMap.put(Arrays.asList(SKEL_LEFT_FOOT, 'y', '+'), 'T');
    buzzerMap.put(Arrays.asList(SKEL_LEFT_FOOT, 'y', '-'), 'T');
    
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_FOOT, 'x', '+'), 'A');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_FOOT, 'x', '-'), 'B');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_FOOT, 'y', '+'), 'B');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_FOOT, 'y', '-'), 'B');
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
void buzzMoves(int joint, float mx, float my, float mz) {
    println("Move joint "+ joint + "  x: " + mx + "  y: " + my + "  z: " + mz);
    buzzMove(joint, 'x', mx);
    buzzMove(joint, 'y', my);
    buzzMove(joint, 'z', mz);
}

void buzzMove(int ijoint, char coordinate, float value) {
    char joint=(char)ijoint;
    // Something's gonna buzz!
    char direction = (value > 0.0) ? '+' : '-';
    Character buzzer = buzzerMap.get(Arrays.asList(joint, coordinate, direction));
    println("joint="+joint+", coordinate="+coordinate+",direction="+direction+",buzzer="+buzzer);
    if (buzzer != null) {
      println("BUZZ: "+buzzer);
      if (bluetooth || serial)
	  port.write(buzzer);
    }
}

void BuzzAll() {
    char c;
    for (c='A';c<'E';c++)
	port.write(c);
}

  public static void main(String args[]) {
    PApplet.main(new String[] { "--present", "MyProcessingSketch" });
  }

}
