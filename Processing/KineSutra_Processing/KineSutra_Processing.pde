import SimpleOpenNI.*;

SimpleOpenNI  kinect;
PrintWriter logger;

int NUM_JOINTS = 15;

int [] jointIDs;
int[] priorJoint = {1,8,8,2,3,8,5,6,-1,8,9,10,8,12,13};
Boolean referenceJointsAreSet = false;
float referenceJointPositions[][] = new float[NUM_JOINTS][3];
float movementVectors[][] = new float[NUM_JOINTS][3];

void setup() {
    size(1024, 768);
    kinect = new SimpleOpenNI(this);
    kinect.enableDepth();
    kinect.enableUser(SimpleOpenNI.SKEL_PROFILE_ALL);
    kinect.setMirror(true);

    strokeWeight(5);
    logger=createWriter("poses.txt");
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
    jointIDs=joints;
}

int liveX=0, liveY=0;

void draw() {
    background(0);
    kinect.update();
    image(kinect.depthImage(), liveX,liveY);
    
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
	translate(liveX,liveY);
	drawSkeleton(currentUser);        
	popMatrix();
    } else {
        text("Set reference pose by pressing 'r'", 40, height - 100);
    }
}


void drawSkeleton(int userId) {
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_HEAD, SimpleOpenNI.SKEL_NECK);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_LEFT_SHOULDER);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_LEFT_ELBOW);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, SimpleOpenNI.SKEL_LEFT_HAND);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_RIGHT_SHOULDER);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_RIGHT_ELBOW);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_ELBOW, SimpleOpenNI.SKEL_RIGHT_HAND);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_TORSO);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_TORSO);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_LEFT_HIP);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_HIP, SimpleOpenNI.SKEL_LEFT_KNEE);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_KNEE, SimpleOpenNI.SKEL_LEFT_FOOT);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_RIGHT_HIP);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_HIP, SimpleOpenNI.SKEL_RIGHT_KNEE);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_KNEE, SimpleOpenNI.SKEL_RIGHT_FOOT);
    kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_HIP, SimpleOpenNI.SKEL_LEFT_HIP);
}


void log(String s) {
    float tm=minute()*60+second()+millis()/1000.0;
    logger.println(tm+","+s);
}


void processSkeletonFromCurrentFrame(int userId) {
    float currentJointPositions[][] = new float[NUM_JOINTS][3];

    for (int joint = 0; joint < jointIDs.length; joint++) {

        PVector jointVector = new PVector();

        kinect.getJointPositionSkeleton(userId, jointIDs[joint], jointVector);
        
        currentJointPositions[joint][0] = jointVector.x;
        currentJointPositions[joint][1] = jointVector.y;
        currentJointPositions[joint][2] = jointVector.z;
        
        // swap this out for function that calculates translated vectors
	if (priorJoint[joint]==-1)
	    for (int k=0;k<3;k++)
		movementVectors[joint][k] = referenceJointPositions[joint][k] - currentJointPositions[joint][k];
	else {
	    float[] relative=new float[3];
	    float[] refrelative=new float[3];
	    float s2=0, s2ref=0;
	    for (int k=0;k<3;k++) {
		relative[k] = currentJointPositions[joint][k]-currentJointPositions[priorJoint[joint]][k];
		refrelative[k] = referenceJointPositions[joint][k]-referenceJointPositions[priorJoint[joint]][k];
		s2+=(relative[k]*relative[k]);
		s2ref+=(refrelative[k]*refrelative[k]);
	    }
	    float llen=sqrt(s2);
	    float refllen=sqrt(s2ref);
	    for (int k=0;k<3;k++) 
		movementVectors[joint][k] = relative[k] - refrelative[k]*llen/refllen;
	    log(joint + "," + currentJointPositions[joint][0]+","+ currentJointPositions[joint][1]+","+ currentJointPositions[joint][2]+","
		+ referenceJointPositions[joint][0]+","+ referenceJointPositions[joint][1]+","+ referenceJointPositions[joint][2]+","
		+ movementVectors[joint][0]+","+ movementVectors[joint][1]+","+ movementVectors[joint][2]);
	}
	logger.flush();
    }            
}

Boolean shouldSendCurrentMovementVectors() {
    
    // replace with logic that determines whether our movement vectors are over a threshold and thus we should send them
    return referenceJointsAreSet;
}

void sendCurrentMovementVectors() {
    float thresh=200;   // Threshold in mm of how much to be off in position to get feedback
    for (int joint=0;joint<jointIDs.length;joint++)
	if (priorJoint[joint]!=-1)
	    if (abs(movementVectors[joint][0])>thresh || abs(movementVectors[joint][1])>thresh || abs(movementVectors[joint][2])>thresh )
		directMoves(joint,movementVectors[joint][0],movementVectors[joint][1],movementVectors[joint][2]);
}

void directMoves(int joint, float mx, float my, float mz) {
    println("Move joint "+ joint + "  x: " + mx + "  y: " + my + "  z: " + mz);
}

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

void drawLimb(int userId, int jointType1, int jointType2)
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
	saveFrame("stayin_alive_"+random(100)+".png");      
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
    println("start pose detection");
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




