import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;


public class Pose {
	
	// Which joint is upstream of this joint (or -1 if none)
	public static int[] PRIOR_JOINTS = {1,8,8,2,3,8,5,6,-1,8,9,10,8,12,13};
	
	// Joint indexes (different from SimpleOpenNI numbering)
	public static char SKEL_HEAD = 0, 
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
	
	public static String JOINT_NAMES[]={"Head","Neck","Left Shoulder","Left Elbow","Left Hand","Right Shoulder","Right Elbow","Right Hand","Torso","Left Hip","Left Foot","Left Knee","Right Hip","Right Knee","Right Foot"};
	
	private PApplet parent;
	private PVector[] positions;
	private PVector[] movementVectors;
	private PImage depthImage;
	private PImage rgbImage;

	public Pose(PApplet parent) {
		this.parent = parent;
	}
	
	
	public void updateMovementVectors(Pose refPose) {
		PVector[] refPositions = refPose.getPositions();
		if (refPositions == null || refPositions.length != positions.length) {
			parent.println("refPositions mismatch");
			return;
		}
	
		if (movementVectors == null) {
			movementVectors = new PVector[refPositions.length];
		}
		
		for (int joint = 0; joint < refPositions.length; joint++) {
			if (PRIOR_JOINTS[joint]==-1) {
				// This joint is not relative to any other joints, set movement to 0
				movementVectors[joint] = new PVector(0,0,0);
			} else {
				// Relative joint
				// Movement is amount to make this joint's position relative to its 'priorJoint' (the one it is relative to) equal to the same relationship in the reference
				// Also, scale limb lengths in case the reference had different length limbs
				PVector relative=new PVector();
				PVector refrelative=new PVector();
				relative = PVector.sub(positions[joint],positions[PRIOR_JOINTS[joint]]);
				refrelative = PVector.sub(refPose.getPositions()[joint],refPose.getPositions()[PRIOR_JOINTS[joint]]);
				float llen=relative.mag();
				float refllen=refrelative.mag();

				if (joint==1)
					parent.println("llen="+llen+",reflen="+refllen);  // Debugging - was seeing some bad lengths

				float refscale=llen/refllen;   //  Amount to scale reference limb to match limb lengths;
				// Sanity check -- otherwise can end up with some very bad movements
				if (refscale<0.8 || refscale >1.3) {
					parent.println("Not scaling reference limb "+joint+","+PRIOR_JOINTS[joint]+" by out of range value " + refscale);
					refscale=(float) 1.0;
				}

				movementVectors[joint] = PVector.sub(PVector.mult(refrelative,refscale),relative);
			}

			// Compute target positions from movement vectors
			//targetVectors[joint]=PVector.add(currentJointPositions[joint],movementVectors[joint]);

			/*
			// Log to data file for post-analysis
			log(sample+","+joint + "," + currentJointPositions[joint].x+","+ currentJointPositions[joint].y+","+ currentJointPositions[joint].z+","
					+ refPose.getPositions()[joint].x+","+ refPose.getPositions()[joint].y+","+ refPose.getPositions()[joint].z+","
					+ movementVectors[joint].x+","+ movementVectors[joint].y+","+ movementVectors[joint].z);
					*/
		}            
	}
	
	public PVector[] targetVectors() {
		if (positions == null || movementVectors == null) {
			return null;
		}
		
		PVector[] targetVectors = new PVector[positions.length];
		
		for (int joint = 0; joint < positions.length; joint++) {
			if (positions[joint] == null || movementVectors[joint] == null) {
				if (positions[joint] == null) {
					positions[joint] = new PVector(0,0,0);
					parent.println("positions was null");
				} else if (movementVectors[joint] == null) {
					movementVectors[joint] = new PVector(0,0,0);
					parent.println("movementVectors was null");
				}
			} 
			targetVectors[joint] = PVector.add(positions[joint], movementVectors[joint]);	
			
		}
		return targetVectors;
	}
	
	
	// #####################################################
	// Setters and Accessors
	// #####################################################
	
	public void setPositions(PVector[] p) { 
		positions = new PVector[p.length];
		for (int j=0;j<p.length;j++) {
			positions[j]=p[j].get(); 
		}
	}
	
	public PVector[] getPositions() {
		return this.positions;
	}
	
	public void setMovementVectors(PVector[] p) { 
		movementVectors = new PVector[p.length];
		for (int j=0;j<p.length;j++) {
			movementVectors[j]=p[j].get(); 
		}
	}
	
	public PVector[] getMovementVectors() {
		return this.movementVectors;
	}
	
	public void setDepthImage(PImage newDepthImage) {
		if (newDepthImage == null) {
			return;
		}
		this.depthImage = parent.createImage(newDepthImage.width, newDepthImage.height, newDepthImage.format);
		this.depthImage.loadPixels();
		parent.arrayCopy(newDepthImage.pixels, this.depthImage.pixels);
		this.depthImage.updatePixels();		
	}	
	
	public PImage getDepthImage() {
		return this.depthImage;
	}
	
	public void setRgbImage(PImage newRgbImage) {
		if (newRgbImage == null) {
			return;
		}
		this.rgbImage = parent.createImage(newRgbImage.width, newRgbImage.height, newRgbImage.format);
		this.rgbImage.loadPixels();
		parent.arrayCopy(newRgbImage.pixels, this.rgbImage.pixels);
		this.rgbImage.updatePixels();		
	}	
	
	public PImage getRgbImage() {
		return this.rgbImage;
	}
	
}
