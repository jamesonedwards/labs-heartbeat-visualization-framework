package util;

import processing.core.PApplet;

import ddf.minim.*;

public class MinimUtil {
	// Make ctor private so you can only use this class statically.
	private MinimUtil() {

	}

	/**
	 * Draw a waveform based on an AudioOutput object.
	 * 
	 * @param pApplet
	 * @param minim
	 * @param audioOutput
	 * @param waveformMultiplier
	 * @param waveformXOffest
	 * @param waveformYOffest
	 * @param leftRightSpread
	 * @param strokeRgb
	 */
	public static void drawWaveform(PApplet pApplet, Minim minim, AudioOutput audioOutput, int waveformMultiplier, float waveformXOffest,
			float waveformYOffest, float leftRightSpread, int[] strokeRgb) {
		/*
		 * We draw the waveform by connecting neighbor values with a line/ We multiply each of the values by 50 because the values in the buffers are
		 * normalized. This means that they have values between -1 and 1. If we don't scale them up our waveform will look more or less like a
		 * straight line.
		 * 
		 * Source: http://code.compartmental.net/tools/minim/quickstart/
		 */
		pApplet.stroke(strokeRgb[0], strokeRgb[1], strokeRgb[2]);
		for (int i = 0; i < audioOutput.bufferSize() - 1; i++) {
			pApplet.line(waveformXOffest + i, waveformMultiplier + audioOutput.left.get(i) * waveformMultiplier + waveformYOffest, i + 1
					+ waveformXOffest, waveformMultiplier + audioOutput.left.get(i + 1) * waveformMultiplier + waveformYOffest);
			pApplet.line(waveformXOffest + i, leftRightSpread + audioOutput.right.get(i) * waveformMultiplier + waveformYOffest, i + 1
					+ waveformXOffest, leftRightSpread + audioOutput.right.get(i + 1) * waveformMultiplier + waveformYOffest);
		}
	}
}
