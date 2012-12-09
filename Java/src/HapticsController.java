import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import processing.core.PApplet;


public class HapticsController {
	
	public Map<List<Character>, Character> buzzerMap;   // Mapping from joint moves to buzzer commands
	private PApplet parent;
	
	public HapticsController(PApplet parent) {
		this.parent = parent;
		setBuzzerMappings();
	}
	
	/**
	 * Returns the joint movement messages for each axis
	 * 
	 * @param joint Joint number
	 * @param mx Movement on the x-axis
	 * @param my Movement on the y-axis
	 * @param mz Movement on the z-axis
	 */
	public ArrayList <Character> getHapticsMessagesForMovement(int joint, float mx, float my, float mz) {
		parent.println("Move joint "+ joint + "  x: " + mx + "  y: " + my + "  z: " + mz);
		
		ArrayList<Character> messageArray = new ArrayList<Character>();
		Character x = hapticsMessageForCoordinateAxis(joint, 'x', mx);
		if (x != null) {
			messageArray.add(x);
		}
		Character y = hapticsMessageForCoordinateAxis(joint, 'y', my);
		if (y != null) {
			messageArray.add(y);
		}
		Character z = hapticsMessageForCoordinateAxis(joint, 'z', mz);
		if (z != null) {
			messageArray.add(z);
		}						
		
		return messageArray;
	}
	
	public ArrayList <Character> getHapticsMessagesForAllJoints() {
		ArrayList <Character> messageList = new ArrayList<Character>();
		char c;
		for (c='A';c<'E';c++) // currently not returning all joints...
			messageList.add(new Character(c));
		
		return messageList;
	}
	
	private Character hapticsMessageForCoordinateAxis(int ijoint, char coordinate, float value) {
		char joint=(char)ijoint;
		// Something's gonna buzz!
		char direction = (value > 0.0) ? '+' : '-';
		Character buzzer = buzzerMap.get(Arrays.asList(joint, coordinate, direction));
		parent.println("joint="+joint+", coordinate="+coordinate+",direction="+direction+",buzzer="+buzzer);
		return buzzer;
	}
	
	/**
	 * Sets the buzzer mappings.
	 */
	private void setBuzzerMappings() {
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

}
