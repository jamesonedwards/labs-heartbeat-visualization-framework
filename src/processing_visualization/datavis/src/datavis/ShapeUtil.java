package datavis;

import processing.core.PApplet;

import java.awt.Color;
import java.awt.geom.Point2D;
import org.apache.commons.lang3.StringUtils;

public class ShapeUtil {
	public static final int DEFAULT_STROKE_WEIGHT = 1;
	public static final int[] DEFAULT_STROKE_COLOR = new int[] { 0, 0, 0 };
	public static final int DEFAULT_ALPHA_MAX = 255;
	private static final float lineSpacingMultiplier = (float) 0.45;

	// Make ctor private so you can only use this class statically.
	private ShapeUtil() {

	}

	/**
	 * Get a point on the circumference of a circle based on the angle in
	 * degrees.
	 * 
	 * @param radius
	 * @param angleInDegrees
	 * @param origin
	 * @return
	 */
	public static Point2D.Float getPointOnCircle(float radius, float angleInDegrees, Point2D.Float origin) {
		float x = (float) (radius * Math.cos(angleInDegrees * Math.PI / 180F)) + origin.x;
		float y = (float) (radius * Math.sin(angleInDegrees * Math.PI / 180F)) + origin.y;
		return new Point2D.Float(x, y);
	}

	/**
	 * Get the width and height of a rectangle containing text, based on the
	 * font size, the number of lines of text and internal padding.
	 * 
	 * @param lines
	 * @param textSize
	 * @param padLeft
	 * @param padRight
	 * @param padTop
	 * @param padBottom
	 * @return
	 */
	public static float[] getTooltipDimensions(PApplet pApplet, String[] lines, int textSize, float padLeft, float padRight, float padTop, float padBottom) {
		// Find longest line.
		float maxLine = 0;
		for (int i = 0; i < lines.length; i++) {
			if (pApplet.textWidth(lines[i]) > maxLine) {
				maxLine = pApplet.textWidth(lines[i]);
			}
		}

		float rectWidth = padLeft + maxLine + padRight;
		float rectHeight = padTop + (textSize * (1 + lineSpacingMultiplier) * lines.length) + padBottom;
		return new float[] { rectWidth, rectHeight };
	}

	/**
	 * Draw a circle on the Processing Applet, based on the Dot object.
	 * 
	 * @param pApplet
	 * @param dot
	 */
	public static void drawDot(PApplet pApplet, Dot dot) {
		drawCircle(pApplet, dot.getCenter().x, dot.getCenter().y, dot.getRadius(), dot.getInnerRgb(), dot.getOuterRgb(), dot.getStrokeWeight(), dot.getAlpha());
	}

	/**
	 * Draw a circle on the Processing Applet. Note: stroke == {-1} is a flag
	 * that means "no stroke".
	 * 
	 * @param pApplet
	 * @param x
	 * @param y
	 * @param radius
	 * @param fill
	 */
	public static void drawCircle(PApplet pApplet, float x, float y, float radius, int[] fill, int[] stroke, int strokeWeight, float alpha) {
		if (stroke.length == 3) {
			pApplet.strokeWeight(strokeWeight);
			pApplet.stroke(stroke[0], stroke[1], stroke[2]);
		} else {
			pApplet.noStroke();
		}
		pApplet.fill(fill[0], fill[1], fill[2], alpha);
		pApplet.ellipse(x, y, radius * 2, radius * 2);
		pApplet.stroke(DEFAULT_STROKE_COLOR[0], DEFAULT_STROKE_COLOR[1], DEFAULT_STROKE_COLOR[2]);
		pApplet.strokeWeight(DEFAULT_STROKE_WEIGHT);
	}

	public static void drawCircle(PApplet pApplet, float x, float y, float radius, int[] fill) {
		drawCircle(pApplet, x, y, radius, fill, DEFAULT_STROKE_COLOR, DEFAULT_STROKE_WEIGHT, DEFAULT_ALPHA_MAX);
	}

	public static void drawRectangleWithText(PApplet pApplet, float x, float y, int curve, float padLeft, float padRight, float padTop, float padBottom, int[] rectRgb, int[] textRgb, int textSize,
			String[] lines) {
		drawRectangleWithText(pApplet, x, y, curve, padLeft, padRight, padTop, padBottom, rectRgb, textRgb, textSize, lines, false, false);
	}

	/**
	 * Draw a rectangle on the Processing Applet and place text inside it.
	 * 
	 * @param pApplet
	 * @param x
	 * @param y
	 * @param curve
	 * @param padLeft
	 * @param padRight
	 * @param padTop
	 * @param padBottom
	 * @param rectRgb
	 * @param textRgb
	 * @param textSize
	 * @param lines
	 * @param alightRight
	 * @param alightBottom
	 */
	public static void drawRectangleWithText(PApplet pApplet, float x, float y, int curve, float padLeft, float padRight, float padTop, float padBottom, int[] rectRgb, int[] textRgb, int textSize,
			String[] lines, boolean alignRight, boolean alignBottom) {
		// Calculate the rectangle dimensions based on the amount of text and
		// the font size.
		float[] rectDims = ShapeUtil.getTooltipDimensions(pApplet, lines, textSize, padLeft, padRight, padTop, padBottom);
		float rectWidth = rectDims[0];
		float rectHeight = rectDims[1];

		// If the rectangle is right or bottom aligned, adjust the x or y
		// coordinate accordingly.
		float trueX = alignRight ? x - rectWidth - 1 : x; // - 1 accounts for
															// the stroke width.
		float trueY = alignBottom ? y - rectHeight + 1 : y; // + 1 accounts for
															// the
		// stroke width.

		String text = StringUtils.join(lines, "\n");
		pApplet.fill(rectRgb[0], rectRgb[1], rectRgb[2]);
		pApplet.rect(trueX, trueY, rectWidth, rectHeight, curve);
		pApplet.textSize(textSize);
		pApplet.fill(textRgb[0], textRgb[1], textRgb[2]);
		pApplet.text(text, trueX + padLeft, trueY + padTop);
	}

	/**
	 * Convert a string hex color to RGB values.
	 * 
	 * @param colorStr
	 *            (e.g. "#FFFFFF")
	 * @return
	 */
	public static int[] hexToRgb(String colorStr) throws IllegalArgumentException {
		if (!colorStr.substring(0, 1).equals("#")) {
			throw new IllegalArgumentException("Value passed to hexToRgb() should begin with \"#\". Given: " + colorStr);
		}
		Color c = Color.decode(colorStr);
		return new int[] { c.getRed(), c.getGreen(), c.getBlue() };
	}
}
