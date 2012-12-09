
// KineSutra processing
//
// November 3, 2012
//
// Paul Mans
// Brent Townshend
// Islam El-Ashi

import java.io.PrintWriter;
import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PVector;
import processing.serial.Serial;
import SimpleOpenNI.IntVector;
import SimpleOpenNI.SimpleOpenNI;
import SimpleOpenNI.SimpleOpenNIConstants;
import controlP5.ControlP5;
import hypermedia.net.*;

/**
 * The Class Kinesutra.
 */
public class Kinesutra extends PApplet {

	// TODO:  
	// 	- add wifiConnection class
	//	- move skeleton drawing into pose class
	// 	- add refPoseList with filmstrip
	// 	- restore logger functionality?


	private static final long serialVersionUID = 1L;
	SimpleOpenNI  kinect;
	PrintWriter logger;
	
	Serial port;
	UDP udp;
	int udpSendPort = 10553;
	int udpListenPort = 10554;
	String udpHostIP = "localhost";

	int [] jointIDs;
	
	public static final int HAPTIC_CONNECTION_MODE_NONE = 0;
	public static final int HAPTIC_CONNECTION_MODE_BLUETOOTH = 1;
	public static final int HAPTIC_CONNECTION_MODE_SERIAL = 2;
	public static final int HAPTIC_CONNECTION_MODE_UDP = 3;
	
	int hapticConnectionMode = HAPTIC_CONNECTION_MODE_UDP;

	
	Pose currPose = null;
	Pose refPose = null;
	ArrayList <Pose> refPoseList;
	
	
	HapticsController hapticsController;
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
		
		if (hapticConnectionMode == this.HAPTIC_CONNECTION_MODE_BLUETOOTH)
			port = new Serial(this, "/dev/tty.FireFly-5F27-SPP", 115200);
		else if (hapticConnectionMode == this.HAPTIC_CONNECTION_MODE_SERIAL) {
			// List all the available serial ports:
			println(Serial.list());

			// Open the port you are using at the rate you want:
			port = new Serial(this, Serial.list()[12], 115200);
		} else if (hapticConnectionMode == this.HAPTIC_CONNECTION_MODE_UDP) {
			updateUDPConnection();
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

		hapticsController = new HapticsController(this);

		// Start time reference
		sday=day();
		shour=hour();
		smin=minute();
		ssec=second();
		smillis=millis();
		
		refPoseList = new ArrayList<Pose>();
		
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

			if (shouldProcessCurrentFrame()) {
				processSkeletonFromCurrentFrame(currentUser);				
				updateAndSendCurrentMovementVectors();
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

	// Decide if haptic updates should be sent yet
	Boolean shouldProcessCurrentFrame() {
		float now=elapsed();
		if ((now<0 || (now-lastHapticUpdate) > hapticUpdateInterval) && refPose != null) {
			lastHapticUpdate=now;
			return true;
		}
		return false;
	}
	
	// Process current skeleton position to determine movements needed
		void processSkeletonFromCurrentFrame(int userId) {
			
			currPose.updateMovementVectors(refPose);
			
			logger.flush();
			sample=sample+1;
		}


	/**
	 * Send current movement vectors to buzzers (one at a time)
	 */
	void updateAndSendCurrentMovementVectors() {
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
		
		if (updatingJoint != -1) {
			// Buzz the current joint
			println("Move "+Pose.JOINT_NAMES[updatingJoint]+" by "+movementVectors[updatingJoint].x+","+movementVectors[updatingJoint].y+","+movementVectors[updatingJoint].z);
			
			ArrayList<Character> movementMessages = hapticsController.getHapticsMessagesForMovement(updatingJoint,movementVectors[updatingJoint].x,movementVectors[updatingJoint].y,movementVectors[updatingJoint].z);
			
			if (hapticConnectionMode == this.HAPTIC_CONNECTION_MODE_BLUETOOTH || hapticConnectionMode == this.HAPTIC_CONNECTION_MODE_SERIAL) {
				for (int i = 0; i < movementMessages.size(); i++) {
					println("BUZZ: "+ movementMessages.get(i));					
					port.write(movementMessages.get(i));
				}		
			} else if (hapticConnectionMode == this.HAPTIC_CONNECTION_MODE_UDP) {
				
				if (movementMessages.size() > 0) {
					Integer millis = new Integer(millis());
					String udpMessage = millis.toString();
					for (int i = 0; i < movementMessages.size(); i++) {
						udpMessage = udpMessage + "," + movementMessages.get(i);
					}
					
					println("sending message: " + udpMessage);
					byte[] buffer = udpMessage.getBytes();
					
					if (!udp.send(buffer, udpHostIP, udpSendPort)) {
						println("failed to send to " + udpHostIP + " on port " + udpSendPort + ": " + udpMessage);
					}
				}							
			}			
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

	public static void main(String args[]) {
		PApplet.main(new String[] { "Kinesutra" });
	}
	
	
	// UDP Handler
	void receive( byte[] data, String ip, int port ) {  // <-- extended handler
	   
	  String message = new String( data );

	  println("Message from UDP connection: " + message);
	  
	}
	
	void updateUDPConnection() {
		// create a new datagram connection
		udp = new UDP( this, udpListenPort); 
		udp.log( true );
		//udp.listen( true ); // also listen to incoming messages
	}
	
}
