package datavis;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;

public class Dot {
	private Point2D.Float center;
	private float radius;
	private int[] innerRgb;
	private int[] outerRgb;
	private int strokeWeight;
	private float alpha;

	public Point2D.Float getCenter() {
		return center;
	}

	public void setCenter(Point2D.Float center) {
		this.center = center;
	}

	public float getRadius() {
		return radius;
	}

	public void setRadius(float radius) {
		this.radius = radius;
	}

	public int[] getInnerRgb() {
		return innerRgb;
	}

	public void setInnerRgb(int[] innerRgb) {
		this.innerRgb = innerRgb;
	}

	public int[] getOuterRgb() {
		return outerRgb;
	}

	public void setOuterRgb(int[] outerRgb) {
		this.outerRgb = outerRgb;
	}

	public int getStrokeWeight() {
		return strokeWeight;
	}

	public void setStrokeWeight(int strokeWeight) {
		this.strokeWeight = strokeWeight;
	}

	public float getAlpha() {
		return alpha;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	// Making this ctor private so that you have to pass params from external
	// call.
	private Dot() {

	}

	public Dot(Float center, float radius, int[] rgb) {
		this.center = center;
		this.radius = radius;
		this.innerRgb = rgb;
		this.outerRgb = ShapeUtil.DEFAULT_STROKE_COLOR;
		this.strokeWeight = ShapeUtil.DEFAULT_STROKE_WEIGHT;
	}

	public Dot(Float center, float radius, int[] innerRgb, int[] outerRgb, int strokeWeight, float alpha) {
		this.center = center;
		this.radius = radius;
		this.innerRgb = innerRgb;
		this.outerRgb = outerRgb;
		this.strokeWeight = strokeWeight;
		this.alpha = alpha;
	}
	
	/**
	 * Is the given point within this dot?
	 * 
	 * Logic and source: 1. Subtract center x from mouseX; square that 2.
	 * Subtract center y from mousey; square that 3. Add the two square values
	 * 4. If the sum is <= square of radius, the mouse is over (in) the circle.
	 * 
	 * http://forum.processing.org/topic/math-details-of-mouse-rollover-circle
	 * 
	 * @param point
	 * @return
	 */
	public boolean containsPoint(Point2D.Float point) {
		if (Math.pow(point.x - center.x, 2) + Math.pow(point.y - center.y, 2) <= Math.pow(radius, 2)) {
			return true;
		} else {
			return false;
		}
	}
}
