
// KineSutra processing
//
// November 3, 2012
//
// Paul Mans
// Brent Townshend
// Islam El-Ashi

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.serial.Serial;
import SimpleOpenNI.IntVector;
import SimpleOpenNI.SimpleOpenNI;
import SimpleOpenNI.SimpleOpenNIConstants;
import controlP5.ControlP5;

/**
 * The Class Kinesutra.
 */
public class Kinesutra extends PApplet {
	// This code need serious refactoring to make it less spaghetti
	// Should have separate class for all the joint position arrays
	// Should use vector class underneath for math operations on the joint positions

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	SimpleOpenNI  kinect;
	PrintWriter logger;
	Serial port;

	int NUM_JOINTS = 15;

	int [] jointIDs;
	Boolean bluetooth = false;
	Boolean serial=false;

	
	Pose currPose = null;
	Pose refPose = null;

	Map<List<Character>, Character> buzzerMap;   // Mapping from joint moves to buzzer commands
	int threshold = 200;  // Threshold for requiring movement (mm)

	int sample=0;   // Current sample number (primarily for logging)
	int sday,shour,smin,ssec,smillis;   // Start time of program
	float hapticUpdateInterval = (float) 1.0;   // Update haptics at this interval in seconds
	float lastHapticUpdate;	   // Elapsed time since start of last haptic update
	int updatingJoint=-1;	   // Which joint is currently being updated
	boolean useDepth=false;
	ControlP5 cp5;

	/* (non-Javadoc)
	 * @see processing.core.PApplet#setup()
	 */
	@Override
	public void setup() {
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
		kinect.enableUser(SimpleOpenNIConstants.SKEL_PROFILE_ALL);
		kinect.setMirror(true);
		cp5=new ControlP5(this);
		cp5.addSlider("threshold").setPosition(20,500).setSize(40,200).setRange(10,500).setValue(200);
		cp5.addToggle("useDepth").setPosition(100,680).setSize(50,20) ;
		logger=createWriter("poses"+year()+"_"+month()+"_"+day()+"_"+hour()+"_"+minute()+".txt");

		// Would've been nice to initialize this above, but causes link errors to reference these before calling the SimpleOpenNI constructor or other methods
		// Initialize a temporary
		int [] joints = new int[]{SimpleOpenNIConstants.SKEL_HEAD, 
				SimpleOpenNIConstants.SKEL_NECK, 
				SimpleOpenNIConstants.SKEL_LEFT_SHOULDER,
				SimpleOpenNIConstants.SKEL_LEFT_ELBOW,
				SimpleOpenNIConstants.SKEL_LEFT_HAND,
				SimpleOpenNIConstants.SKEL_RIGHT_SHOULDER,
				SimpleOpenNIConstants.SKEL_RIGHT_ELBOW,
				SimpleOpenNIConstants.SKEL_RIGHT_HAND,
				SimpleOpenNIConstants.SKEL_TORSO,
				SimpleOpenNIConstants.SKEL_LEFT_HIP,
				SimpleOpenNIConstants.SKEL_LEFT_KNEE,
				SimpleOpenNIConstants.SKEL_LEFT_FOOT,
				SimpleOpenNIConstants.SKEL_RIGHT_HIP,
				SimpleOpenNIConstants.SKEL_RIGHT_KNEE,
				SimpleOpenNIConstants.SKEL_RIGHT_FOOT};
		// Copy into global
		jointIDs=joints;

		setBuzzerMappings();

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
		return (float) (((((day()-sday)*24+(hour()-shour))*60)+(minute()-smin))*60+(second()-ssec)+(millis()-smillis)/1000.0);
	}

	int currentUser;

	/* (non-Javadoc)
	 * Processing main draw loop
	 * @see processing.core.PApplet#draw()
	 */
	@Override
	public void draw() {
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

		if (refPose != null) {
			pushMatrix();
			if (useDepth)
				image(refPose.getDepthImage(),640,0,320,240);
			else
				image(refPose.getRgbImage(),640,0,320,240);
			
			translate(640,240);
			scale((float) 0.5);
			stroke(0,255,0);
			strokeWeight(2);
			drawLimbs(refPose.getPositions());
			popMatrix();
		}
		// draw the skeleton in whatever color we chose
		if (currentUser > 0) {
			if (currPose == null) {
				currPose = new Pose(this);
			}
			
			// Load current joints from Kinect
			PVector[] jointVectors = new PVector[jointIDs.length];
			for (int joint = 0; joint < jointIDs.length; joint++) {
				PVector jointVector = new PVector();

				kinect.getJointPositionSkeleton(currentUser, jointIDs[joint], jointVector);
				jointVectors[joint] = jointVector;
			}
			currPose.setPositions(jointVectors);

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

		if (updatingJoint == -1 && refPose != null)  {
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
		kinect.drawLimb(currentUser, SimpleOpenNIConstants.SKEL_HEAD, SimpleOpenNIConstants.SKEL_NECK);

		stroke(255,0,0);
		drawLimbs(currPose.getPositions());
		if (refPose != null) {
			stroke(0,255,0);
			drawLimbs(currPose.targetVectors());
		}
	}

	// Draw all limbs using joint points in p
	void drawLimbs(PVector p[]) {
		drawLimb(p,Pose.SKEL_HEAD, Pose.SKEL_NECK);
		drawLimb(p,Pose.SKEL_NECK, Pose.SKEL_LEFT_SHOULDER);
		drawLimb(p,Pose.SKEL_LEFT_SHOULDER, Pose.SKEL_LEFT_ELBOW);
		drawLimb(p,Pose.SKEL_LEFT_ELBOW, Pose.SKEL_LEFT_HAND);
		drawLimb(p,Pose.SKEL_NECK, Pose.SKEL_RIGHT_SHOULDER);
		drawLimb(p,Pose.SKEL_RIGHT_SHOULDER, Pose.SKEL_RIGHT_ELBOW);
		drawLimb(p,Pose.SKEL_RIGHT_ELBOW, Pose.SKEL_RIGHT_HAND);
		drawLimb(p,Pose.SKEL_LEFT_SHOULDER, Pose.SKEL_TORSO);
		drawLimb(p,Pose.SKEL_RIGHT_SHOULDER, Pose.SKEL_TORSO);
		drawLimb(p,Pose.SKEL_TORSO, Pose.SKEL_LEFT_HIP);
		drawLimb(p,Pose.SKEL_LEFT_HIP, Pose.SKEL_LEFT_KNEE);
		drawLimb(p,Pose.SKEL_LEFT_KNEE, Pose.SKEL_LEFT_FOOT);
		drawLimb(p,Pose.SKEL_TORSO, Pose.SKEL_RIGHT_HIP);
		drawLimb(p,Pose.SKEL_RIGHT_HIP, Pose.SKEL_RIGHT_KNEE);
		drawLimb(p,Pose.SKEL_RIGHT_KNEE, Pose.SKEL_RIGHT_FOOT);
		drawLimb(p,Pose.SKEL_RIGHT_HIP, Pose.SKEL_LEFT_HIP);
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
		
		currPose.updateMovementVectors(refPose);
		
		logger.flush();
		sample=sample+1;
	}

	// Decide if haptic updates should be sent yet
	Boolean shouldSendCurrentMovementVectors() {
		float now=elapsed();
		if ((now<0 || (now-lastHapticUpdate) > hapticUpdateInterval) && refPose != null) {
			lastHapticUpdate=now;
			return true;
		}
		return false;
	}

	/**
	 * Send current movement vectors to buzzers (one at a time)
	 */
	void sendCurrentMovementVectors() {
		PVector[] movementVectors = currPose.getMovementVectors();
		
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
			println("Move "+Pose.JOINT_NAMES[updatingJoint]+" by "+movementVectors[updatingJoint].x+","+movementVectors[updatingJoint].y+","+movementVectors[updatingJoint].z);
			buzzMoves(updatingJoint,movementVectors[updatingJoint].x,movementVectors[updatingJoint].y,movementVectors[updatingJoint].z);
		}
	}

	/**
	 * Sets the reference joint positions.
	 *
	 * @param userId user ID of the user whose skeleton should be used
	 */
	void setReferencePosition(int userId) {
		
		PVector[] jointVectors = new PVector[jointIDs.length];
		
		for (int joint = 0; joint < jointIDs.length; joint++) {
			PVector jointVector = new PVector();

			kinect.getJointPositionSkeleton(userId, jointIDs[joint], jointVector);
			
			jointVectors[joint] = jointVector;

			println("Joint "+ joint + "  x: " + jointVector.x + "  y: " + jointVector.y + "  z: " + jointVector.z);
		}
		
		refPose = new Pose(this); // always instantiate a new pose so we can keep a ref to the old one in our poseList 
		refPose.setPositions(jointVectors);
		refPose.setDepthImage(kinect.depthImage());
		refPose.setRgbImage(kinect.rgbImage());
	}


	/* (non-Javadoc)
	 * @see processing.core.PApplet#keyPressed()
	 */
	@Override
	public void keyPressed(){
		if (key == 's') {
			saveFrame("capture_"+random(100)+".png");      
		}

		if (key == 'r') {
			IntVector userList = new IntVector();
			kinect.getUsers(userList);
			if (userList.size() > 0 &&  kinect.isTrackingSkeleton(userList.get(0))) {
				println("Acquiring reference");
				setReferencePosition(userList.get(0));
				println("Reference acquired");
			} else {
				println("Not tracking user - no reference acquired");
			}      
		}
	}

	/**
	 * Callback from SimpleOpenNI on new user.
	 *
	 * @param userId the user id
	 */
	public void onNewUser(int userId) {
		println("New user "+userId+": start pose detection");
		kinect.startPoseDetection("Psi", userId);
	}

	public void onLostUser(int userId) {
		println("Lost user "+userId);
	}
	
	public void onStartCalibration(int userId) {
		println("Starting calibration of user "+userId);
	}
	
	public void onEndCalibration(int userId, boolean successful) {
		if (successful) { 
			println("Calibrated user "+userId);
			kinect.startTrackingSkeleton(userId);
		} 
		else { 
			println("Failed to calibrate user "+userId);
			kinect.startPoseDetection("Psi", userId);
		}
	}

	public void onStartPose(String pose, int userId) {
		println("Started pose "+pose+" for user "+userId);
		kinect.stopPoseDetection(userId); 
		kinect.requestCalibrationSkeleton(userId, true);
	}

	public void onEndPose(String pose, int userId) {
		println("Ending pose "+pose+" for user "+userId);
	}
	
	/**
	 * Sets the buzzer mappings.
	 */
	void setBuzzerMappings() {
		// Buzz mappings (joint, coordinate, direction) => Buzzer code
		buzzerMap = new HashMap<List<Character>, Character>();

		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_SHOULDER, 'x', '+'), 'A');
		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_SHOULDER, 'x', '-'), 'B');

		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_SHOULDER, 'x', '+'), 'C');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_SHOULDER, 'x', '-'), 'D');

		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_HAND, 'x', '+'), 'E');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_HAND, 'x', '-'), 'F');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_HAND, 'y', '+'), 'G');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_HAND, 'y', '-'), 'H');

		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_HAND, 'x', '+'), 'R');
		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_HAND, 'x', '-'), 'Q');
		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_HAND, 'y', '+'), 'R');
		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_HAND, 'y', '-'), 'Q');

		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_ELBOW, 'x', '+'), 'H');
		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_ELBOW, 'x', '-'), 'G');
		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_ELBOW, 'y', '+'), 'H');
		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_ELBOW, 'y', '-'), 'G');

		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_ELBOW, 'x', '+'), 'I');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_ELBOW, 'x', '-'), 'J');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_ELBOW, 'y', '+'), 'I');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_ELBOW, 'y', '-'), 'J');

		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_ELBOW, 'x', '+'), 'a');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_ELBOW, 'x', '-'), 'a');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_ELBOW, 'y', '+'), 'B');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_ELBOW, 'y', '-'), 'B');

		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_HIP, 'x', '+'), 'a');
		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_HIP, 'x', '-'), 'a');

		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_HIP, 'x', '+'), 'B');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_HIP, 'x', '-'), 'B');

		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_KNEE, 'x', '+'), 'O');
		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_KNEE, 'x', '-'), 'P');
		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_KNEE, 'y', '+'), 'O');
		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_KNEE, 'y', '-'), 'P');

		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_KNEE, 'x', '+'), 'M');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_KNEE, 'x', '-'), 'K');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_KNEE, 'y', '+'), 'M');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_KNEE, 'y', '-'), 'K');

		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_FOOT, 'x', '+'), 'T');
		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_FOOT, 'x', '-'), 'T');
		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_FOOT, 'y', '+'), 'T');
		buzzerMap.put(Arrays.asList(Pose.SKEL_LEFT_FOOT, 'y', '-'), 'T');

		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_FOOT, 'x', '+'), 'A');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_FOOT, 'x', '-'), 'B');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_FOOT, 'y', '+'), 'B');
		buzzerMap.put(Arrays.asList(Pose.SKEL_RIGHT_FOOT, 'y', '-'), 'B');
	}

	/**
	 * Directs a joint move to a buzzer.
	 * 
	 * @param joint Joint number
	 * @param mx Movement on the x-axis
	 * @param my Movement on the y-axis
	 * @param mz Movement on the z-axis
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
		PApplet.main(new String[] { "Kinesutra" });
	}

}
