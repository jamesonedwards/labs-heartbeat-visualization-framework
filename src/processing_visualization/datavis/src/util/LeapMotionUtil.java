package util;

import processing.core.PApplet;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.InteractionBox;
import com.leapmotion.leap.Vector;

public class LeapMotionUtil {
	public static final int LEFT_HAND = 1;
	public static final int RIGHT_HAND = 2;

	// Make ctor private so you can only use this class statically.
	private LeapMotionUtil() {

	}

	/**
	 * Convert the Leap Position position Vector to a Vector in the Processing window coordinate system.
	 * 
	 * @param pApplet
	 * @param lmController
	 * @param leapVector
	 * @return
	 */
	public static Vector leapToProcessingVector(PApplet pApplet, Controller lmController, Vector leapVector) {
		return leapToProcessingVectorHelper(pApplet, lmController, leapVector, 0, 1, 0, pApplet.width);
	}

	/**
	 * Convert the Leap Position position Vector to a Vector in the Processing window coordinate system, splitting the space between the left and
	 * right hands.
	 * 
	 * @param pApplet
	 * @param lmController
	 * @param leapVector
	 * @param hand
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static Vector leapToProcessingVector(PApplet pApplet, Controller lmController, Vector leapVector, int hand)
			throws IllegalArgumentException {
		if (hand != LEFT_HAND && hand != RIGHT_HAND)
			throw new IllegalArgumentException("Hand must be either LeapMotionUtil.LEFT_HAND or LeapMotionUtil.RIGHT_HAND.");

		float pxMin = 0;
		float pxMax = 0;
		float lxMin = 0;
		float lxMax = 0;

		switch (hand) {
		case LEFT_HAND:
			pxMin = 0;
			pxMax = pApplet.width / 2;
			lxMin = 0;
			lxMax = 0.5f;
			break;
		case RIGHT_HAND:
			pxMin = pApplet.width / 2;
			pxMax = pApplet.width;
			lxMin = 0.5f;
			lxMax = 1.0f;
			break;
		}

		return leapToProcessingVectorHelper(pApplet, lmController, leapVector, lxMin, lxMax, pxMin, pxMax);
	}

	private static Vector leapToProcessingVectorHelper(PApplet pApplet, Controller lmController, Vector leapVector, float lxMin, float lxMax,
			float pxMin, float pxMax) {
		// Normalize the coordinates.
		InteractionBox iBox = lmController.frame().interactionBox();
		Vector iBoxVector = iBox.normalizePoint(leapVector);

		if (iBoxVector.getX() < lxMin)
			iBoxVector.setX(lxMin);
		else if ((iBoxVector.getX() > lxMax))
			iBoxVector.setX(lxMax);

		// Center points around the center of window, since the Leap origin is the center. Y is negative since coordinate system is different.
		return new Vector(PApplet.map(iBoxVector.getX(), lxMin, lxMax, pxMin, pxMax), PApplet.map(iBoxVector.getY(), 0, 1, pApplet.height, 0),
				iBoxVector.getZ());
	}
}
