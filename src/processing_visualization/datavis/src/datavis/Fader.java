package datavis;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

/**
 * A fader is the root class of all fade effects It should draw a monochrome
 * image into the alphamask PImage which is used as a mask (black=original
 * image, white=new image, grey=proportional blend of original and new images).
 * Okay, to be more accurate, the mask uses ARGB and just uses the B channel.
 * But if you use fill(N) and stroke(N) to get a greyscale color, it won't
 * matter.
 * 
 * SOURCE: http://www.openprocessing.org/sketch/13104
 */

public abstract class Fader {
	protected PApplet pApplet;
	protected PImage img1; // start image
	protected PImage img2; // end image
	protected PGraphics alphamask; // monochrome image mask

	// Make default ctor private so it can't be called externally.
	protected Fader() {

	}

	public Fader(PApplet pApplet) {
		this.pApplet = pApplet;
	}

	public void setImages(PImage im1, PImage im2) {
		img1 = im1;
		img2 = im2;
		alphamask = pApplet.createGraphics(img1.width, img1.height, PApplet.P2D);
		startFade();
	}

	/**
	 * called to initialise fade. This default implementation starts with a
	 * black mask. Subclasses can override this to do other initialisation.
	 */
	public void startFade() {
		alphamask.beginDraw();
		alphamask.fill(0);
		alphamask.stroke(0);
		alphamask.rect(0, 0, img1.width, img1.height);
		alphamask.endDraw();
	}

	/**
	 * called to end the fade. This default implementation creates a 100% white
	 * mask. May not be needed, but I've had some problems with rounding errors
	 * and arc drawing leaving gaps. Calling this will guarantee you see 100%
	 * final image.
	 */
	public void endFade() {
		alphamask.beginDraw();
		alphamask.fill(255);
		alphamask.stroke(255);
		alphamask.rect(0, 0, img1.width, img1.height);
		alphamask.endDraw();
	}

	/**
	 * called for each frame of the animation. pass in the percentage (0.0-1.0)
	 * of the effect. this should be overridden for each effect subclass.
	 */
	public abstract void animateFade(float percent);

	/**
	 * Renders blended images. You shouldn't need to override this.
	 */
	public void draw() {
		pApplet.loadPixels();
		int w = img1.width;
		for (int ix = 0; ix < img1.pixels.length; ix++) {
			int y = ix / w;
			int x = ix % w;
			int col1 = img1.pixels[ix];
			int r1 = (col1 & 0xFF0000) >> 16;
			int g1 = (col1 & 0xFF00) >> 8;
			int b1 = (col1 & 0xFF);
			int col2 = img2.pixels[ix];
			int r2 = (col2 & 0xFF0000) >> 16;
			int g2 = (col2 & 0xFF00) >> 8;
			int b2 = (col2 & 0xFF);
			int colmask = alphamask.pixels[ix] ^ 0xFFFFFFFF;
			int level = (colmask & 0xFF); // use blue channel
			float lev = (float) level / 256.0f;
			int rr = (int) ((float) (r1 * lev) + (float) (r2 * (1 - lev)));
			int gg = (int) ((float) (g1 * lev) + (float) (g2 * (1 - lev)));
			int bb = (int) ((float) (b1 * lev) + (float) (b2 * (1 - lev)));
			pApplet.pixels[ix] = 0xFF000000 | rr << 16 | gg << 8 | bb;
		}

		pApplet.updatePixels();
	}
}