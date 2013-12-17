package datavis;

import processing.core.PApplet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import java.awt.geom.Point2D;
import java.util.logging.*;
import controlP5.*;

public class Datavis extends PApplet {
	// Global constants:
	private static final String DATA_URL = "http://127.0.0.1:8000/viewevents/";
	// TODO: CHANGE THIS TO 10 SECONDS: Roughly 10 seconds at 60 fps.
	private static final int DATA_REFRESH_THRESHOLD = 60 * 10;
	private static final int MAIN_WINDOW_WIDTH = 1200;
	private static final int MAIN_WINDOW_HEIGHT = 800;
	private static final int DOT_BUFFER = 10;
	private static final int DOT_DEFAULT_RADIUS = 5;
	private static final int MAIN_CIRCLE_TICK_LENGTH = 5;

	// TODO: Refine this:
	private static boolean BLOB_HACK_ON = true;

	// Global colors:
	private static final int[] BACKGROUND_RGB = new int[] { 255, 255, 255 };
	private static final int[] MAIN_CIRCLE_RGB = new int[] { 200, 200, 200 };
	private static final int[] TOOLTIP_RGB = new int[] { 240, 240, 240 };
	private static final int[] KEY_RECT_RGB = new int[] { 240, 240, 240 };
	private static final int[] TEXT_RGB = new int[] { 0, 102, 153 };

	// Logging:
	private final static Logger LOGGER = Logger.getLogger("HeartbeatEventLogger");
	private final static Level LOG_LEVEL = Level.ALL;

	// Global variables:
	private static Hashtable<String, List<HeartbeatEvent>> heartbeatEventGroups;
	private static Hashtable<String, HeartbeatEventSetConfig> heartbeatEventSetConfigGroups;
	private static Hashtable<String, int[]> eventTagRgb;
	private static Hashtable<String, ArrayList<String>> eventSetTags;
	private static HeartbeatEventSet allHeartbeatEvents;
	private static float centerX;
	private static float centerY;
	private static float radius;
	private static int currentDateView = -1;
	private static int dataRefreshCounter = 0;
	private static boolean displayEventsByTag = false;
	//public static ControlP5 cp5;

	public void setup() {
		// Setup logging.
		LOGGER.setLevel(LOG_LEVEL);
		LOGGER.addHandler(new ConsoleHandler());

		// Set up Applet.
		size(MAIN_WINDOW_WIDTH, MAIN_WINDOW_HEIGHT);
		background(BACKGROUND_RGB[0], BACKGROUND_RGB[1], BACKGROUND_RGB[2]);
		heartbeatEventGroups = new Hashtable<String, List<HeartbeatEvent>>();
		heartbeatEventSetConfigGroups = new Hashtable<String, HeartbeatEventSetConfig>();
		eventTagRgb = new Hashtable<String, int[]>();
		eventSetTags = new Hashtable<String, ArrayList<String>>();
		refreshData();

		//cp5 = new ControlP5(this);
		//Group g1 = cp5.addGroup("CURRENT VIEW").setPosition(10, 10).setBackgroundHeight(400).setBackgroundColor(color(255, 50));
		//cp5.addButton("ButtonA").setGroup(g1);
	}

	public void draw() {
		// Redraw background so old tooltips don't linger.
		background(BACKGROUND_RGB[0], BACKGROUND_RGB[1], BACKGROUND_RGB[2]);

		if (dataRefreshCounter >= DATA_REFRESH_THRESHOLD) {
			// Reset the counter and request the latest data from the API.
			long[] range = getTimestampRange();
			refreshData(range[0], range[1]);
			dataRefreshCounter = 0;
		} else {
			dataRefreshCounter++;
		}

		// Draw the main circle.
		drawMainCircle();

		for (int i = 0; i < allHeartbeatEvents.getAllowedEventKeys().length; i++) {
			String key = allHeartbeatEvents.getAllowedEventKeys()[i];
			HeartbeatEventSetConfig config = heartbeatEventSetConfigGroups.get(key);
			if (config.getEnabled()) {
				renderEventSet(heartbeatEventGroups.get(key), config.getRgbValues(), i + 1);
			}
		}

		// Check if mouse is hovered over a dot.
		checkForDotHover();

		// Draw the instructions for the visualization.
		drawDataSetKey();
		drawCurrentViewKey();
	}

	// TODO: This should eventually be controlled by the UI somehow.
	public void keyPressed() {
		LOGGER.info("Key was pressed: " + key);
		// Determine what to do when certain keys are pressed:
		// 1 - #: toggles datasets 1 = # on/off
		// 0: turn all datasets on
		// a, y, m, d, h: toggles time range between all/year/month/day/hour
		String configKey;
		long[] range;

		switch (key) {
		case ('j'):
			BLOB_HACK_ON = !BLOB_HACK_ON;
		case ('0'):
			for (int i = 0; i < allHeartbeatEvents.getAllowedEventKeys().length; i++) {
				String k = allHeartbeatEvents.getAllowedEventKeys()[i];
				if (heartbeatEventSetConfigGroups.containsKey(k) && heartbeatEventSetConfigGroups.get(k) != null) {
					heartbeatEventSetConfigGroups.get(k).setEnabled(true);
				}
			}
			// Disable the global flag to display dots by tag.
			displayEventsByTag = false;
			break;
		case ('s'):
			String screenshotName = "screenshots/screenshot_" + Calendar.getInstance().getTimeInMillis() + ".jpg"; 
			save(screenshotName);
			// FIXME: Alert user that the image has been saved.
		case ('a'):
			currentDateView = -1;
			refreshData();
			break;
		case ('y'):
			currentDateView = Calendar.YEAR;
			range = getTimestampRange();
			refreshData(range[0], range[1]);
			break;
		case ('m'):
			currentDateView = Calendar.MONTH;
			range = getTimestampRange();
			refreshData(range[0], range[1]);
			break;
		case ('d'):
			currentDateView = Calendar.DAY_OF_YEAR;
			range = getTimestampRange();
			refreshData(range[0], range[1]);
			break;
		case ('h'):
			currentDateView = Calendar.HOUR_OF_DAY;
			range = getTimestampRange();
			refreshData(range[0], range[1]);
			break;
		default:
			// If the key is a number in the range from 1 and the number of
			// datasets, toggle that dataset.
			// Note: this is a single character so only 1-9 are possible.
			try {
				int iKey = Integer.parseInt(String.valueOf(key));
				if (iKey > 0 && iKey <= allHeartbeatEvents.getAllowedEventKeys().length) {
					configKey = allHeartbeatEvents.getAllowedEventKeys()[iKey - 1];
					heartbeatEventSetConfigGroups.get(configKey).toggleEnabled();
				}
			} catch (Exception ex) {
				// Ignore.
			}
		}
	}

	public void mouseClicked() {
		checkForDotClick();
	}

	private void drawMainCircle() {
		centerX = width / 2;
		centerY = height / 2 + 50;
		radius = width / 5;
		ShapeUtil.drawCircle(this, centerX, centerY, radius, MAIN_CIRCLE_RGB);
		line(centerX, centerY - radius - MAIN_CIRCLE_TICK_LENGTH, centerX, centerY + radius + MAIN_CIRCLE_TICK_LENGTH);
		line(centerX - radius - MAIN_CIRCLE_TICK_LENGTH, centerY, centerX + radius + MAIN_CIRCLE_TICK_LENGTH, centerY);
	}

	private void refreshData() {
		refreshData(-1, -1);
	}

	private void refreshData(long timestamp_start, long timestamp_end) {
		LOGGER.info("Refreshing data.");
		try {
			// TODO: This will leave config sets that may no longer exist. Fix
			// this eventually.

			// Get all heartbeat events.
			String url = DATA_URL;
			if (timestamp_start > 0 && timestamp_end > 0) {
				url += "?timestamp_start=" + timestamp_start + "&timestamp_end=" + timestamp_end;
			}
			String strData = UrlUtil.readUrl(url);
			allHeartbeatEvents = new Gson().fromJson(strData, HeartbeatEventSet.class);

			// Clear the existing sets.
			heartbeatEventGroups.clear();

			// Refresh event set keys.
			for (int i = 0; i < allHeartbeatEvents.getAllowedEventKeys().length; i++) {
				String key = allHeartbeatEvents.getAllowedEventKeys()[i];
				heartbeatEventGroups.put(key, new ArrayList<HeartbeatEvent>());
				// Keep enabled setting.
				boolean enabled = true; // The default.
				if (heartbeatEventSetConfigGroups.containsKey(key) && heartbeatEventSetConfigGroups.get(key) != null) {
					// If this key already exists and is non-null, use the
					// existing enabled value.
					enabled = heartbeatEventSetConfigGroups.get(key).getEnabled();
				}
				heartbeatEventSetConfigGroups.put(key, new HeartbeatEventSetConfig(key, HeartbeatEventSetConfig.DOT_COLORS[i], enabled));
			}

			// Loop through new data dump and separate into datasets.
			for (Iterator<HeartbeatEvent> i = allHeartbeatEvents.getAllHeartbeatEvents().iterator(); i.hasNext();) {
				HeartbeatEvent item = i.next();
				heartbeatEventGroups.get(item.getEventKey()).add(item);
			}
		} catch (Exception ex) {
			LOGGER.severe("Unable to read data from URL. Details: " + ex.getMessage());
		}
	}

	private long[] getTimestampRange() {
		long start = -1;
		long end = -1;
		// If the current date view is -1 ("all"), then return -1, -1. Else,
		// return proper TS range.
		if (currentDateView >= 0) {
			Calendar cal = Calendar.getInstance();
			end = floor(cal.getTimeInMillis() / 1000); // Remove milliseconds.
			cal.add(currentDateView, -1); // Subtract the time increment
											// (year/month/day/hour).
			start = floor(cal.getTimeInMillis() / 1000); // Remove milliseconds.
		}
		return new long[] { start, end };
	}

	private void checkForDotHover() {
		// TODO: Optimize this with a separate hashtable index?
		for (Iterator<HeartbeatEvent> i = allHeartbeatEvents.getAllHeartbeatEvents().iterator(); i.hasNext();) {
			HeartbeatEvent item = i.next();
			// Note: Check that the dot exists.
			if (item.getInnerDot() != null && item.getInnerDot().containsPoint(new Point2D.Float(mouseX, mouseY))) {
				// LOGGER.info("Hovered over dot: " + item.getEventValue());
				displayEventToolip(item);
				// If we're over a dot, exit the loop so we don't trigger
				// another tooltip.
				break;
			}
		}
	}

	private void checkForDotClick() {
		for (Iterator<HeartbeatEvent> i = allHeartbeatEvents.getAllHeartbeatEvents().iterator(); i.hasNext();) {
			HeartbeatEvent item = i.next();
			// Note: Check that the dot exists.
			if (item.getInnerDot() != null && item.getInnerDot().containsPoint(new Point2D.Float(mouseX, mouseY))) {
				LOGGER.info("Clicked on dot: " + item.getEventValue());

				// If left mouse button was clicked, display only the clicked
				// dataset, and display a different color for each tag.
				if (mouseButton == LEFT) {
					// Disable all datasets excepted for clicked one.
					for (int j = 0; j < allHeartbeatEvents.getAllowedEventKeys().length; j++) {
						String key = allHeartbeatEvents.getAllowedEventKeys()[j];
						if (heartbeatEventSetConfigGroups.containsKey(key) && heartbeatEventSetConfigGroups.get(key) != null) {
							heartbeatEventSetConfigGroups.get(key).setEnabled(key.equals(item.getEventKey()));
						}
					}

					// Set the global flag to display dots by tag.
					displayEventsByTag = true;

					BLOB_HACK_ON = false;
				}

				// If we clicked on a dot, exit the loop.
				break;
			}
		}
	}

	private void displayEventToolip(HeartbeatEvent heartbeatEvent) {
		String[] lines = new String[] { heartbeatEvent.getEventKey(), heartbeatEvent.getEventValue(), "Tags: " + StringUtils.join(heartbeatEvent.getEventTags(), ", "),
				heartbeatEvent.getEventDatetime() };
		Point2D.Float center = heartbeatEvent.getInnerDot().getCenter();
		// Draw a rectangle and place text inside it. Place the rectangle above
		// and to the right of the mouse position.
		int rectCurve = 7;
		float padLeft = 10;
		float padRight = 10;
		float padTop = 20;
		float padBottom = 0;
		float rectOuterPadLeft = 5;
		float rectOuterPadTop = 5;
		int textSize = 12;
		ShapeUtil.drawRectangleWithText(this, center.x + rectOuterPadLeft, center.y - rectOuterPadTop, rectCurve, padLeft, padRight, padTop, padBottom, TOOLTIP_RGB, TEXT_RGB, textSize, lines, false,
				true);
	}

	/**
	 * Draw dots around the main circle.
	 * 
	 * @param heartbeatEvents
	 * @param fill
	 * @param stackHeight
	 */
	private void renderEventSet(List<HeartbeatEvent> heartbeatEvents, int[] fill, int stackHeight) {
		float[] tmpArcPoints = new float[] { -1, -1 };
		Point2D.Float prevPt = null;

		for (Iterator<HeartbeatEvent> i = heartbeatEvents.iterator(); i.hasNext();) {
			HeartbeatEvent item = i.next();
			int[] stroke = ShapeUtil.DEFAULT_STROKE_COLOR;

			// Map the timestamps to the circle angle (in degrees).
			float mappedTS = map(item.getEventTimestamp(), allHeartbeatEvents.getTimestampMin(), allHeartbeatEvents.getTimestampMax(), 0, 360);

			// Get the mapped point on the circle. We want the points to hover
			// just outside the circle, so increase the radius by a buffer.
			float outerRadius = radius + (DOT_DEFAULT_RADIUS + DOT_BUFFER) * stackHeight;
			Point2D.Float curPt = ShapeUtil.getPointOnCircle(outerRadius, mappedTS, new Point2D.Float(centerX, centerY));

			if (BLOB_HACK_ON) {
				// If this is the first point in the set, mark the start of the
				// blob.
				if (prevPt == null) {
					tmpArcPoints[0] = radians(mappedTS);
					prevPt = new Point2D.Float(curPt.x, curPt.y);
				}
				// Else if this is the last point in the set, mark the end of
				// the blob.
				else if (!i.hasNext()) {
					tmpArcPoints[1] = radians(mappedTS);
					closeAndDrawBlob(tmpArcPoints, 2 * outerRadius, fill);
				}
				// Else if this dot overlaps the previous one, mark it as the
				// blob end.
				else if (curPt.distance(prevPt) <= (double) (2 * DOT_DEFAULT_RADIUS)) {
					tmpArcPoints[1] = radians(mappedTS);
					prevPt = new Point2D.Float(curPt.x, curPt.y);
				}
				// Else if this dot doesn't overlap the previous one, draw the
				// currently set blob and then start a new one with this dot.
				else if (curPt.distance(prevPt) > (double) (2 * DOT_DEFAULT_RADIUS)) {
					closeAndDrawBlob(tmpArcPoints, 2 * outerRadius, fill);
					tmpArcPoints = new float[] { radians(mappedTS), -1 };
					prevPt = new Point2D.Float(curPt.x, curPt.y);
				}

				// Set stroke to {-1} as a special flag to remove stroke from
				// dots below.
				stroke = new int[] { -1 };
			}

			// Create a Dot object for that point and add to the HeartbeatEvent
			// object.
			Dot dot;

			// If we're looking by tag, then assign stroke colors different for
			// each tag. Else keep the same color for the entire dataset.
			if (displayEventsByTag) {
				// Use only the first tag.
				String tag = item.getEventTags()[0];
				String eventKey = item.getEventKey();
				stroke = getRgbForTag(tag);

				// Add to eventSetTags map.
				if (!eventSetTags.containsKey(eventKey)) {
					eventSetTags.put(eventKey, new ArrayList<String>());
				}
				if (!eventSetTags.get(eventKey).contains(tag)) {
					eventSetTags.get(eventKey).add(tag);
				}

				// When arranging by tag, swap the stroke and fill colors.
				dot = new Dot(curPt, DOT_DEFAULT_RADIUS, stroke, fill, 2, ShapeUtil.DEFAULT_ALPHA_MAX);
			} else {
				dot = new Dot(curPt, DOT_DEFAULT_RADIUS, fill, stroke, ShapeUtil.DEFAULT_STROKE_WEIGHT, ShapeUtil.DEFAULT_ALPHA_MAX);
			}

			// Add dot to heartbeat event.
			item.setInnerDot(dot);

			// Draw the dot.
			ShapeUtil.drawDot(this, dot);
		}
	}

	private void closeAndDrawBlob(float[] arcPoints, float radius, int[] rgb) {
		// LOGGER.info("closeAndDrawBlob() called with start = " + arcPoints[0]
		// + " and end = " + arcPoints[1]);
		if (arcPoints[0] > 0 && arcPoints[1] > 0 && arcPoints[0] != arcPoints[1]) {
			// Draw an arc line that is the thickness of the dot.
			noFill();
			stroke(rgb[0], rgb[1], rgb[2]);
			// Add 2 pixels to the blob line weight for visual affect.
			strokeWeight(2 * DOT_DEFAULT_RADIUS + 2); // TODO: should be 2* or
														// 3*?
			arc(centerX, centerY, radius, radius, arcPoints[0], arcPoints[1]);

			// Revert stroke values to default.
			stroke(ShapeUtil.DEFAULT_STROKE_COLOR[0], ShapeUtil.DEFAULT_STROKE_COLOR[1], ShapeUtil.DEFAULT_STROKE_COLOR[2]);
			strokeWeight(ShapeUtil.DEFAULT_STROKE_WEIGHT);
		}
	}

	private int[] getRgbForTag(String tag) {
		// FIXME: Find a better way to pick colors.
		if (!eventTagRgb.containsKey(tag)) {
			int[] rgb = new int[] { floor((float) Math.random() * 255), floor((float) Math.random() * 255), floor((float) Math.random() * 255) };
			eventTagRgb.put(tag, rgb);
		}
		return eventTagRgb.get(tag);
	}

	private void drawCurrentViewKey() {
		String strCurView;
		switch (currentDateView) {
		case Calendar.YEAR:
			strCurView = "Year";
			break;
		case Calendar.MONTH:
			strCurView = "Month";
			break;
		case Calendar.DAY_OF_YEAR:
			strCurView = "Day";
			break;
		case Calendar.HOUR_OF_DAY:
			strCurView = "Hour";
			break;
		default:
			strCurView = "All Time";
		}

		List<String> lines = new ArrayList<String>();
		lines.add("CURRENT VIEW:");
		lines.add(strCurView);
		lines.add("");
		lines.add("CURRENT DATASETS:");

		String eventKeyForDisplayByTag = "";

		for (int i = 0; i < allHeartbeatEvents.getAllowedEventKeys().length; i++) {
			String key = allHeartbeatEvents.getAllowedEventKeys()[i];
			HeartbeatEventSetConfig config = heartbeatEventSetConfigGroups.get(key);
			if (config.getEnabled()) {
				lines.add(config.getEventKey());
				// If we're displaying events by tag, there will only be one
				// dataset enabled, so store its key.
				eventKeyForDisplayByTag = config.getEventKey();
			}
		}

		// If we have drilled down into the tags for a dataset, indicate that.
		if (displayEventsByTag) {
			lines.add("");
			lines.add("TAGS:");
			lines.add(StringUtils.join(eventSetTags.get(eventKeyForDisplayByTag), ", "));
		}

		String[] arrLines = lines.toArray(new String[lines.size()]);
		int textSize = 12;
		float padLeft = 10;
		float padRight = 10;
		float padTop = 20;
		float padBottom = 5;
		float x = 0;
		float y = 0;
		int rectCurve = 0;
		int[] rectFill = KEY_RECT_RGB;
		int[] textFill = TEXT_RGB;
		ShapeUtil.drawRectangleWithText(this, x, y, rectCurve, padLeft, padRight, padTop, padBottom, rectFill, textFill, textSize, arrLines);
	}

	private void drawDataSetKey() {
		int numDatasets = allHeartbeatEvents.getAllowedEventKeys().length;
		String[] lines = new String[] { "KEY COMMANDS:", "a = All Time", "y = Year", "m = Month", "d = Day", "h = Hour", "1 - " + numDatasets + " = Toggle datasets 1 - " + numDatasets,
				"0 = All datasets", "j = Toggle \"join dots\"", "s = Save", "esc = Exit" };
		int textSize = 12;
		float padLeft = 10;
		float padRight = 10;
		float padTop = 20;
		float padBottom = 5;
		float x = width;
		float y = 0;
		int rectCurve = 0;
		int[] rectFill = KEY_RECT_RGB;
		int[] textFill = TEXT_RGB;
		ShapeUtil.drawRectangleWithText(this, x, y, rectCurve, padLeft, padRight, padTop, padBottom, rectFill, textFill, textSize, lines, true, false);
	}
}
