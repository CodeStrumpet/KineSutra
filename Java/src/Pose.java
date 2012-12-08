import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;


public class Pose {
	PApplet parent;
	private PVector[] positions;
	private PImage depthImage;
	private PImage rgbImage;

	public Pose(PApplet parent) {
		this.parent = parent;
	}
	
	public void setPositions(PVector[] p) { 
		positions = new PVector[p.length];
		for (int j=0;j<p.length;j++) {
			positions[j]=p[j]; 
		}
	}
	
	public PVector[] getPositions () {
		return this.positions;
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
