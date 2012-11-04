// KineSutra processing
//
// November 3, 2012
//
// Paul Mans
// Brent Townshend
// Islam El-Ashi

import SimpleOpenNI.*;

// This code need serious refactoring to make it less spaghetti
// Should have separate class for all the joint position arrays
// Should use vector class underneath for math operations on the joint positions

SimpleOpenNI  kinect;
PrintWriter logger;

int NUM_JOINTS = 15;

int [] jointIDs;
Boolean referenceJointsAreSet = false;  // Once we've acquired a reference

// Various vectors of joint positions
float currentJointPositions[][] = new float[NUM_JOINTS][3];
float referenceJointPositions[][] = new float[NUM_JOINTS][3];
float movementVectors[][] = new float[NUM_JOINTS][3];
float targetVectors[][] = new float[NUM_JOINTS][3];

// Which joint is upstream of this joint (or -1 if none)
int[] priorJoint = {1,8,8,2,3,8,5,6,-1,8,9,10,8,12,13};

Map<List<Character>, Character> buzzerMap;   // Mapping from joint moves to buzzer commands
float threshold = 200;  // Threshold for requiring movement (mm)

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
float hapticUpdateInterval = 0.5;   // Update haptics at this interval
float lastHapticUpdate;	   // Elapsed time since start of last haptic update
int updatingJoint=-1;	   // Which joint is currently being updated

// UI layout
int uiImagePosX=0, uiImagePosY=0;    // Position of live image on screen

void setup() {
    frameRate(30);
    size(1024, 768);
    kinect = new SimpleOpenNI(this);
    kinect.enableDepth();
    kinect.enableUser(SimpleOpenNI.SKEL_PROFILE_ALL);
    kinect.setMirror(true);

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

    // Buzz mappings (joint, coordinate, direction) => Buzzer code
    buzzerMap = new HashMap<List<Character>,Character>();
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_FOOT, 'x', '+'), 'a');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_FOOT, 'x', '-'), 'b');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_FOOT, 'z', '+'), 'c');
    buzzerMap.put(Arrays.asList(SKEL_RIGHT_FOOT, 'z', '-'), 'd');

    // Start time reference
    sday=day();
    shour=hour();
    smin=minute();
    ssec=second();
    smillis=millis();
}

// Elapsed time in seconds since startup
// Will roll over after 24 hours of continuous running
float elapsed() {
      return ((((day()-sday)*24+(hour()-shour))*60)+(minute()-smin))*60+(second()-ssec)+(millis()-smillis)/1000.0;
}

// Processing main draw loop
void draw() {
    background(0);
    kinect.update();
    image(kinect.depthImage(), uiImagePosX,uiImagePosY);
    
    int currentUser = -1;
    
    IntVector userList = new IntVector();
    kinect.getUsers(userList);
    if (userList.size() > 0) {
        int userId = userList.get(0);
        if( kinect.isTrackingSkeleton(userId)) {
            currentUser = userId;
        }
    }
    
    // draw the skeleton in whatever color we chose
    if (currentUser > 0) {
        if (shouldSendCurrentMovementVectors()) {
	    processSkeletonFromCurrentFrame(currentUser);
            sendCurrentMovementVectors();
        }
        pushMatrix();
	translate(uiImagePosX,uiImagePosY);
	drawSkeleton();        
	popMatrix();
    } else {
        text("Set reference pose by pressing 'r'", 40, height - 40);
    }
}

// Draw entire skeleton
// Current position in red,  target position in green
void drawSkeleton() {
    strokeWeight(5);
    stroke(255,0,0);
    drawLimbs(currentJointPositions);
    if (referenceJointsAreSet) {
	stroke(0,255,0);
	drawLimbs(targetVectors);
    }
}

// Draw all limbs using joint points in p
void drawLimbs(float p[][]) {
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
void drawLimb(float p[][],int joint1, int joint2) {
    line(p[joint1][0],p[joint1][1],p[joint1][2],p[joint2][0],p[joint2][1],p[joint2][2]);
}


void log(String s) {
    logger.println(elapsed()+","+s);
}

// Process current skeleton position to determine movements needed
void processSkeletonFromCurrentFrame(int userId) {

    for (int joint = 0; joint < jointIDs.length; joint++) {

        PVector jointVector = new PVector();

        kinect.getJointPositionSkeleton(userId, jointIDs[joint], jointVector);
        
        currentJointPositions[joint][0] = jointVector.x;
        currentJointPositions[joint][1] = jointVector.y;
        currentJointPositions[joint][2] = jointVector.z;
        
	if (priorJoint[joint]==-1)
	    // This joint is not relative to any other joints, set movement to 0
	    for (int k=0;k<3;k++)
		movementVectors[joint][k] = 0;
	else {
	    // Relative joint
	    // Movement is amount to make this joint's position relative to its 'priorJoint' (the one it is relative to) equal to the same relationship in the reference
	    // Also, scale limb lengths in case the reference had different length limbs
	    float[] relative=new float[3];
	    float[] refrelative=new float[3];
	    for (int k=0;k<3;k++) {
		relative[k] = currentJointPositions[joint][k]-currentJointPositions[priorJoint[joint]][k];
		refrelative[k] = referenceJointPositions[joint][k]-referenceJointPositions[priorJoint[joint]][k];
	    }
	    float llen=norm(relative);
	    float refllen=norm(refrelative);

	    if (joint==1)
		println("llen="+llen+",reflen="+refllen);  // Debugging - was seeing some bad lengths

	    float refscale=llen/refllen;   //  Amount to scale reference limb to match limb lengths;
	    // Sanity check -- otherwise can end up with some very bad movements
	    if (refscale<0.8 || refscale >1.3) {
		println("Not scaling reference limb "+joint+","+priorJoint[joint]+" by out of range value " + refscale);
		refscale=1.0;
	    }
	    for (int k=0;k<3;k++) 
		movementVectors[joint][k] = refrelative[k]*refscale - relative[k];
	}

	// Compute target positions from movement vectors
	for (int k=0;k<3;k++) 
	    targetVectors[joint][k]=currentJointPositions[joint][k]+movementVectors[joint][k];

	// Log to data file for post-analysis
	log(sample+","+joint + "," + currentJointPositions[joint][0]+","+ currentJointPositions[joint][1]+","+ currentJointPositions[joint][2]+","
	    + referenceJointPositions[joint][0]+","+ referenceJointPositions[joint][1]+","+ referenceJointPositions[joint][2]+","
	    + movementVectors[joint][0]+","+ movementVectors[joint][1]+","+ movementVectors[joint][2]);
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

// L-2 norm of a 3-vector
float norm(float[] vec) {
    return sqrt((vec[0]*vec[0])+(vec[1]*vec[1])+(vec[2]*vec[2]));
}

// Send current movement vectors to buzzers (one at a time)
void sendCurrentMovementVectors() {
    if (updatingJoint==-1 || norm(movementVectors[updatingJoint])<threshold) {
	// Decide which joint should be moving (starting at top and working down a limb at a time)
	// Keep buzzing the same joint until it is within threshold, then go onto the next one that is wrong
	updatingJoint=-1;
	for (int joint=0;joint<jointIDs.length;joint++)
	    if (norm(movementVectors[joint]) > threshold) {
		updatingJoint = joint;
		break;
	    }
    }
    if (updatingJoint!=-1) {
	// Buzz the current joint
	println("Move "+jointNames[updatingJoint]+" by "+movementVectors[updatingJoint][0]+","+movementVectors[updatingJoint][1]+","+movementVectors[updatingJoint][2]);
	buzzMoves(updatingJoint,movementVectors[updatingJoint][0],movementVectors[updatingJoint][1],movementVectors[updatingJoint][2]);
    }
}

// Grab current positions as reference
void setReferenceJointPositions(int userId) {
    for (int joint = 0; joint < jointIDs.length; joint++) {
	PVector jointVector = new PVector();

	kinect.getJointPositionSkeleton(userId, jointIDs[joint], jointVector);
	referenceJointPositions[joint][0] = jointVector.x;
	referenceJointPositions[joint][1] = jointVector.y;
	referenceJointPositions[joint][2] = jointVector.z;        

	println("Joint "+ joint + "  x: " + jointVector.x + "  y: " + jointVector.y + "  z: " + jointVector.z);
    }
    referenceJointsAreSet = true;
}

void OLDdrawLimb(int userId, int jointType1, int jointType2)
{
    PVector jointPos1 = new PVector();
    PVector jointPos2 = new PVector();
    float  confidence;

    // draw the joint position
    confidence = kinect.getJointPositionSkeleton(userId, jointType1, jointPos1);
    confidence = kinect.getJointPositionSkeleton(userId, jointType2, jointPos2);

    line(jointPos1.x, jointPos1.y, jointPos1.z, 
	 jointPos2.x, jointPos2.y, jointPos2.z);
}

void keyPressed(){
    if (key == 's') {
	saveFrame("capture_"+random(100)+".png");      
    }

    if (key == 'r') {
	println("Acquiring reference");
	IntVector userList = new IntVector();
	kinect.getUsers(userList);
	saveFrame("reference.png");      
	if (userList.size() > 0) {
	    if (kinect.isTrackingSkeleton(userList.get(0))) {
		setReferenceJointPositions(userList.get(0));              
	    } else {
		println("Not tracking user 0");
	    }
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

void buzzMove(int joint, int coordinate, float value) {
    // Above the threshold limit. Something's gonna buzz!
    int direction = (value > 0.0) ? '+' : '-';
    Character buzzcode=buzzerMap.get(Arrays.asList(joint, coordinate, direction));
    if (buzzcode != null)
	 println("Buzz: "+buzzcode);
}
