package datavis;

import java.awt.geom.Point2D;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

import processing.core.PApplet;
import processing.core.PGraphics;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class DatavisV2 extends PApplet {
	// Global constants:
	private static final String DATA_URL = "http://localhost.mcgarrybowen.com:8000/viewtwitterevents/?hashtag=";
	private static final String IMAGE_UPLOAD_URL = "http://localhost.mcgarrybowen.com:8000/_upload_flickr_image/";
	private static final String SAVED_IMAGE_DIR = "C:\\Dev\\labs-heartbeat-visualization-framework\\src\\processing_visualization\\datavis\\saved_images\\";
	private static final int MAIN_WINDOW_WIDTH = 1600;
	private static final int MAIN_WINDOW_HEIGHT = 900;
	private static final int DOT_RADIUS_SCALE_MIN = 1;
	private static final int DOT_RADIUS_SCALE_MAX = 100;
	// 50% transparency.
	private static final float DOT_OUTER_ALPHA = ShapeUtil.DEFAULT_ALPHA_MAX / 2;
	// How much bigger the printable image should be.
	private static final float SAVED_IMAGE_RATIO = 5;
	// The format that the printable image should be saved in.
	private static final String SAVED_IMAGE_FORMAT = ".png";
	// Should we include retweets?
	private static final boolean INCLUDE_RETWEETS = true;

	// Global theme parameters:
	// Theme: "red", "blue", "tan".
	private static final String THEME = "tan";
	private static int[] BACKGROUND_RGB;
	private static int[] CENTER_HASHTAG_COLOR;
	private static int[] CENTER_DATE_COLOR;

	// Global colors:
	private static final int[] TOOLTIP_RGB = new int[] { 240, 240, 240 };
	private static final int[] KEY_RECT_RGB = new int[] { 240, 240, 240 };
	private static final int[] TEXT_RGB = new int[] { 0, 102, 153 };

	// Logging:
	private final static Logger LOGGER = Logger.getLogger("HeartbeatEventLogger");
	private final static Level LOG_LEVEL = Level.ALL;

	// Global variables:
	private static Hashtable<String, List<HeartbeatEvent>> heartbeatEventGroups;
	private static HeartbeatEventSet allHeartbeatEvents;
	private static float centerX;
	private static float centerY;
	private static float radius;
	private static List<HeartbeatEvent[]> dotArcs;
	private static Date startDate;
	private static Date endDate;
	private static String flickrImageUrl;

	// Global email config:
	private static final String SMTP_HOST_NAME = "smtp.sendgrid.net";
	private static final String SMTP_AUTH_USER = "sg@mcgarrybowen.com";
	private static final String SMTP_AUTH_PWD = "W6!filVq";
	private static final String ADMIN_EMAIL = "jameson.edwards@mcgarrybowen.com";
	private static final String IMAGE_EMAIL_SUBJECT = "Your data vis image is ready!";
	private static final boolean EMAIL_DEBUG_MODE = true;
	private static MailUtil.Config mailConfig;
	private static MailUtil mailUtil;

	// Global Flickr config:
	private static final String FLICKR_API_KEY = "349b5a85f6cc1894466063777625dd28";
	private static final String FLICKR_API_SECRET = "9bd196c52f3ae061";
	private static final String FLICKR_API_TOKEN = "72157634361392636-0c64d6a5aafc383a";

	// FIXME: Make all of this user-configurable:
	// private static String hashtag = "#wwdc";
	private static String hashtag = "#nyc";
	// private static String hashtag = "#mcgarrybowen";
	private static String startDateStr = "2013-06-22";
	private static String endDateStr = "2013-06-26";
	private static String userEmail = "jameson.edwards@mcgarrybowen.com";

	public void setup() {
		// Setup logging.
		LOGGER.setLevel(LOG_LEVEL);
		LOGGER.addHandler(new ConsoleHandler());

		try {
			// Setup email util (in a separate try-catch block because error
			// handling is different if mail fails.
			mailConfig = new MailUtil.Config(SMTP_HOST_NAME, SMTP_AUTH_USER, SMTP_AUTH_PWD, EMAIL_DEBUG_MODE);
			mailUtil = new MailUtil(mailConfig);
		} catch (Exception ex) {
			LOGGER.severe(ex.getMessage());
			System.exit(1); // Exit program.
		}

		try {
			// Choose theme.
			switch (THEME) {
			case "red":
				BACKGROUND_RGB = new int[] { 255, 0, 0 };
				CENTER_HASHTAG_COLOR = new int[] { 51, 51, 51 };
				CENTER_DATE_COLOR = new int[] { 187, 184, 177 };
				break;
			case "blue":
				BACKGROUND_RGB = new int[] { 20, 139, 199 };
				CENTER_HASHTAG_COLOR = new int[] { 51, 51, 51 };
				CENTER_DATE_COLOR = new int[] { 187, 184, 177 };
				break;
			case "tan":
				BACKGROUND_RGB = new int[] { 238, 234, 227 };
				CENTER_HASHTAG_COLOR = new int[] { 51, 51, 51 };
				CENTER_DATE_COLOR = new int[] { 204, 204, 204 };
				break;
			}

			// Set up Applet.
			size(MAIN_WINDOW_WIDTH, MAIN_WINDOW_HEIGHT);
			setConfig();
			heartbeatEventGroups = new Hashtable<String, List<HeartbeatEvent>>();
			dotArcs = new ArrayList<HeartbeatEvent[]>();
			refreshData();
			if (allHeartbeatEvents.getAllHeartbeatEvents() == null || allHeartbeatEvents.getAllHeartbeatEvents().size() == 0)
				throw new Exception("No events found for this date range.");
		} catch (Exception ex) {
			LOGGER.severe(ex.getMessage());
			emailError(ex);
		}
	}

	public void draw() {
		setConfig();

		// This is a hack since there is only one data set, but whatever.
		for (int i = 0; i < allHeartbeatEvents.getAllowedEventKeys().length; i++) {
			String key = allHeartbeatEvents.getAllowedEventKeys()[i];
			renderEventSet(heartbeatEventGroups.get(key));
		}

		// Check if mouse is hovered over a dot.
		// checkForDotHover();

		// Draw the instructions for the visualization.
		// drawDataSetKey();

		// Draw the center text.
		drawCenterText();
	}

	// TODO: This should eventually be controlled by the UI somehow.
	public void keyPressed() {
		LOGGER.info("Key was pressed: " + key);

		// Determine what to do when certain keys are pressed
		switch (key) {
		case ('s'):
			String screenshotName = SAVED_IMAGE_DIR + "screenshots/" + hashtag + "_" + startDateStr + "_" + endDateStr + "_" + THEME + SAVED_IMAGE_FORMAT;
			PGraphics hires = createGraphics(floor(width * SAVED_IMAGE_RATIO), floor(height * SAVED_IMAGE_RATIO), JAVA2D);
			beginRecord(hires);
			hires.scale(SAVED_IMAGE_RATIO);
			draw();
			endRecord();
			hires.save(screenshotName);
			// Alert user that the image has been saved.
			println("Screenshot saved to " + screenshotName);

			try {
				// Upload the file to Flickr.
				String response = UrlUtil.uploadFile(screenshotName, IMAGE_UPLOAD_URL);

				// Extract the flickr ID from the response and include that in
				// the email to the user.
				FlickrImageResponse flickrImageResponse = new Gson().fromJson(response, FlickrImageResponse.class);
				flickrImageUrl = flickrImageResponse.getImageUrl();

				// Send an email to the user containing the Flickr link.
				emailImageToUser();
			} catch (Exception ex) {
				LOGGER.severe(ex.getMessage());
				emailError(ex);
			}
		}
	}

	private void setConfig() {
		// Redraw background.
		background(BACKGROUND_RGB[0], BACKGROUND_RGB[1], BACKGROUND_RGB[2]);

		// Set hidden circle values.
		centerX = width / 2;
		centerY = height / 2;
		radius = width / 9;
	}

	private int getRetweetWorth(JsonObject tweet) {
		// float multiplier = 500.0f;
		float multiplier = 1.0f;
		return floor(tweet.get("retweet_count").getAsInt() * multiplier);
	}

	private void refreshData() {
		long[] tsRange = getTimestampRange();
		refreshData(tsRange[0], tsRange[1]);
	}

	private void refreshData(long timestamp_start, long timestamp_end) {
		LOGGER.info("Refreshing data.");
		try {
			// Get all heartbeat events for the given date range.
			String url = DATA_URL + URLEncoder.encode(hashtag, "UTF-8");
			if (timestamp_start > 0 && timestamp_end > 0) {
				url += "&timestamp_start=" + timestamp_start + "&timestamp_end=" + timestamp_end;
			}
			String strData = UrlUtil.readUrl(url);
			allHeartbeatEvents = new Gson().fromJson(strData, HeartbeatEventSet.class);

			// Clear the existing sets.
			heartbeatEventGroups.clear();

			// Loop through new data dump and get min and max dot size values.
			int dotInnerRadiusMin = Integer.MAX_VALUE;
			int dotInnerRadiusMax = Integer.MIN_VALUE;
			int dotOuterRadiusMin = Integer.MAX_VALUE;
			int dotOuterRadiusMax = Integer.MIN_VALUE;
			for (Iterator<HeartbeatEvent> i = allHeartbeatEvents.getAllHeartbeatEvents().iterator(); i.hasNext();) {
				HeartbeatEvent item = i.next();
				// TODO: Add some exception handling inside this loop.
				JsonObject tweet = item.getRawData().getAsJsonObject();
				JsonObject user = tweet.getAsJsonObject("user");
				int followerCount = user.get("followers_count").getAsInt() + getRetweetWorth(tweet);
				if (followerCount > dotInnerRadiusMax) {
					dotInnerRadiusMax = followerCount;
				}
				if (followerCount < dotInnerRadiusMin) {
					dotInnerRadiusMin = followerCount;
				}
				int retweetCount = getRetweetWorth(tweet);
				if (retweetCount > dotOuterRadiusMax) {
					dotOuterRadiusMax = retweetCount;
				}
				if (retweetCount < dotOuterRadiusMin) {
					dotOuterRadiusMin = retweetCount;
				}
			}

			// Loop through new data dump and separate into datasets.
			for (Iterator<HeartbeatEvent> i = allHeartbeatEvents.getAllHeartbeatEvents().iterator(); i.hasNext();) {
				HeartbeatEvent item = i.next();
				if (heartbeatEventGroups.get(item.getEventKey()) == null) {
					heartbeatEventGroups.put(item.getEventKey(), new ArrayList<HeartbeatEvent>());
				}
				// TODO: Add some exception handling inside this loop.
				buildDots(item, dotInnerRadiusMin, dotInnerRadiusMax, dotOuterRadiusMin, dotOuterRadiusMax);
				heartbeatEventGroups.get(item.getEventKey()).add(item);
			}

			// Now loop through each data set and for each event, check for a
			// reply-to arc.
			// Nasty O(N^2) loop (the other loop will be short, but at least
			// it's only called once).
			// UPDATE: Adding retweets to the arc algorithm:
			// retweeted_status/id_str
			for (Iterator<List<HeartbeatEvent>> i = heartbeatEventGroups.values().iterator(); i.hasNext();) {
				List<HeartbeatEvent> list = i.next();
				for (Iterator<HeartbeatEvent> i2 = list.iterator(); i2.hasNext();) {
					HeartbeatEvent event1 = i2.next();
					String idStr1 = event1.getRawData().getAsJsonObject().get("id_str").getAsString();
					JsonElement inReplyToStatusId1 = event1.getRawData().getAsJsonObject().get("in_reply_to_status_id_str");
					JsonElement retweetedStatus1 = event1.getRawData().getAsJsonObject().get("retweeted_status");
					JsonElement retweetedStatusId1 = JsonNull.INSTANCE;
					if (retweetedStatus1 != JsonNull.INSTANCE && retweetedStatus1 != null) {
						retweetedStatusId1 = retweetedStatus1.getAsJsonObject().get("id_str");
					}

					if ((inReplyToStatusId1 != JsonNull.INSTANCE && inReplyToStatusId1.getAsString().length() > 0)
							|| (retweetedStatusId1 != JsonNull.INSTANCE && retweetedStatusId1 != null && retweetedStatusId1.getAsString().length() > 0)) {

						String inReplyToStatusIdStr1 = inReplyToStatusId1 != JsonNull.INSTANCE ? inReplyToStatusId1.getAsString() : null;
						String retweetedStatusIdStr1 = retweetedStatusId1 != JsonNull.INSTANCE ? retweetedStatusId1.getAsString() : null;

						for (Iterator<HeartbeatEvent> i3 = list.iterator(); i3.hasNext();) {
							HeartbeatEvent event2 = i3.next();
							boolean isConnection = false;
							String idStr2 = event2.getRawData().getAsJsonObject().get("id_str").getAsString();

							if (idStr1.equals(idStr2)) {
								// If these are the same event, skip this loop
								// iteration.
								continue;
							}

							// Check if there is a reply-to.
							if (inReplyToStatusIdStr1 != null && inReplyToStatusIdStr1.equals(idStr2)) {
								// We have a match.
								isConnection = true;
							} else if (INCLUDE_RETWEETS && retweetedStatusIdStr1 != null && retweetedStatusIdStr1.equals(idStr2)) {
								// We have a match.
								isConnection = true;
							}

							if (isConnection) {
								dotArcs.add(new HeartbeatEvent[] { event1, event2 });
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			LOGGER.severe("Unable to read data from URL. Details: " + ex.getMessage());
		}
	}

	/**
	 * Build the inner and outer dots for this event.
	 * 
	 * @param event
	 */
	private void buildDots(HeartbeatEvent event, int dotInnerRadiusMin, int dotInnerRadiusMax, int dotOuterRadiusMin, int dotOuterRadiusMax) {
		// Map the timestamps to the circle angle (in degrees).
		// Note: use 359 instead of 360 degrees so that first and last dot
		// don't have the same center point.
		float mappedTS = map(event.getEventTimestamp(), allHeartbeatEvents.getTimestampMin(), allHeartbeatEvents.getTimestampMax(), 0, 359);

		// Set the mapped timestamp, which will be used later.
		event.setMappedTimestamp(mappedTS);

		// Set stroke to {-1} as a flag to remove stroke from dots.
		int[] stroke = new int[] { -1 };

		JsonObject tweet = event.getRawData().getAsJsonObject();
		JsonObject user = tweet.getAsJsonObject("user");

		// Get inner dot color from status author's BG color.
		String userBgColor = "#" + user.get("profile_background_color").getAsString();
		int[] innerFill, outerFill;
		innerFill = outerFill = ShapeUtil.hexToRgb(userBgColor);

		// Get inner dot size from the number of followers for the status
		// author.
		int userFollowersCount = user.get("followers_count").getAsInt();

		// Outer dot size is the inner dot size + number of retweets for the
		// status.
		int retweetCount = getRetweetWorth(tweet);

		// Map to a predefined scale. Note that the scale for retweets is
		// smaller to give retweets more weight.
		float innerRadius = map(userFollowersCount, 0, dotInnerRadiusMax, DOT_RADIUS_SCALE_MIN, DOT_RADIUS_SCALE_MAX);
		float outerRadius = innerRadius + map(retweetCount, 0, dotOuterRadiusMax, DOT_RADIUS_SCALE_MIN, DOT_RADIUS_SCALE_MAX / 5);

		// Get the mapped point on the circle. Vary the stack height of the dots
		// based on tweet metrics.
		float dotCenterVariation = (userFollowersCount / 100) + (outerRadius / 2);
		// Make sure the dot doesn't get too far out.
		if (dotCenterVariation > radius / 2)
			dotCenterVariation = radius / 2;
		Point2D.Float point = ShapeUtil.getPointOnCircle(radius + dotCenterVariation, mappedTS, new Point2D.Float(centerX, centerY));

		// Create the inner and outer dots for this point and add to the
		// HeartbeatEvent object.
		Dot innerDot, outerDot;
		innerDot = new Dot(point, innerRadius, innerFill, stroke, ShapeUtil.DEFAULT_STROKE_WEIGHT, ShapeUtil.DEFAULT_ALPHA_MAX);
		// Outer dot color is inner dot color at 50% opacity.
		outerDot = new Dot(point, outerRadius, outerFill, stroke, ShapeUtil.DEFAULT_STROKE_WEIGHT, DOT_OUTER_ALPHA);

		// Add dots to heartbeat event.
		event.setInnerDot(innerDot);
		event.setOuterDot(outerDot);
	}

	private long[] getTimestampRange() {
		// If a date range is used, return proper timestamp, else return -1, -1
		// ("all").

		long start = -1;
		long end = -1;
		try {
			// Convert date strings to date objects.
			if (startDateStr.length() > 0 && endDateStr.length() > 0) {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				startDate = df.parse(startDateStr);
				endDate = df.parse(endDateStr);
				// Remove milliseconds.
				start = floor(startDate.getTime() / 1000);
				end = floor(endDate.getTime() / 1000);
			}
		} catch (ParseException ex) {
			// Warn and move on.
			LOGGER.severe("Unable to parse date range. Using previous 24 hours!");
			Calendar cal = Calendar.getInstance();
			// Now.
			end = floor(cal.getTimeInMillis() / 1000); // Remove milliseconds.
			// Subtract one day.
			cal.add(Calendar.DAY_OF_YEAR, -1);
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

	private void displayEventToolip(HeartbeatEvent heartbeatEvent) {
		// TODO: More text in tooltip?
		String[] lines = new String[] { heartbeatEvent.getEventKey(), heartbeatEvent.getEventValue(), heartbeatEvent.getEventDatetime() };
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
	 */
	private void renderEventSet(List<HeartbeatEvent> heartbeatEvents) {
		for (Iterator<HeartbeatEvent> i = heartbeatEvents.iterator(); i.hasNext();) {
			HeartbeatEvent item = i.next();

			// Draw inner and outer dots.
			ShapeUtil.drawDot(this, item.getInnerDot());
			ShapeUtil.drawDot(this, item.getOuterDot());
		}

		// Draw reply-to arcs.
		for (Iterator<HeartbeatEvent[]> i = dotArcs.iterator(); i.hasNext();) {
			HeartbeatEvent[] events = i.next();
			Point2D.Float start = events[0].getInnerDot().getCenter();
			Point2D.Float end = events[1].getInnerDot().getCenter();
			stroke(events[0].getInnerDot().getInnerRgb()[0], events[0].getInnerDot().getInnerRgb()[1], events[0].getInnerDot().getInnerRgb()[2]);
			noFill();

			// Find center value between the two event angles (in degress).
			float angleCtr = abs(lerp(events[0].getMappedTimestamp(), events[1].getMappedTimestamp(), 0.5f));

			// Get point on a circle with greater radius than main circle.
			float arcHeightMultiplier = 1.75f;
			Point2D.Float controlPoint = ShapeUtil.getPointOnCircle(radius * arcHeightMultiplier, angleCtr, new Point2D.Float(centerX, centerY));
			bezier(start.x, start.y, controlPoint.x, controlPoint.y, controlPoint.x, controlPoint.y, end.x, end.y);
		}

		stroke(ShapeUtil.DEFAULT_STROKE_COLOR[0], ShapeUtil.DEFAULT_STROKE_COLOR[1], ShapeUtil.DEFAULT_STROKE_COLOR[2]);
	}

	/**
	 * Draw the center text (note two colors)
	 */
	private void drawCenterText() {
		int textSize = 12;
		textSize(textSize);
		SimpleDateFormat df = new SimpleDateFormat("EEEE, MMMM d", Locale.US);
		String dateStr = " / " + df.format(startDate) + DateUtil.getDaySuffix(startDate);
		float textWidthDiff = textWidth(dateStr) - textWidth(hashtag);
		float halfWidth = textWidthDiff / 2;
		fill(CENTER_HASHTAG_COLOR[0], CENTER_HASHTAG_COLOR[1], CENTER_HASHTAG_COLOR[2]);
		text(hashtag, centerX - halfWidth - textWidth(hashtag) /*- 2*/, centerY); // Include
																						// buffer.
		fill(CENTER_DATE_COLOR[0], CENTER_DATE_COLOR[1], CENTER_DATE_COLOR[2]);
		text(dateStr, centerX - halfWidth, centerY);
		fill(ShapeUtil.DEFAULT_STROKE_COLOR[0]);
	}

	private void drawDataSetKey() {
		int numDatasets = allHeartbeatEvents.getAllowedEventKeys().length;
		String[] lines = new String[] { "KEY COMMANDS:", "s = Save screenshot", "esc = Exit" };
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

	private void emailImageToUser() {
		try {
			String msg = "Here is your data visualization for " + hashtag + ":\n\n<a href=\"" + flickrImageUrl + "\">" + flickrImageUrl + "</a>\n\n";
			mailUtil.sendSecureEmail(userEmail, ADMIN_EMAIL, null, ADMIN_EMAIL, IMAGE_EMAIL_SUBJECT, msg.replaceAll("(\r\n|\n)", "<br />"));
		} catch (Exception ex) {
			LOGGER.severe("FATAL ERROR: Could not email image to user. Details: " + ex.getMessage());
			System.exit(1); // Exit program.
		}
	}

	private void emailError(Exception ex) {
		try {
			String msg = "Exception details: \nType: " + ex.getClass().toString() + "\nMessage: " + ex.getMessage() + "\nStack trace: " + ExceptionUtils.getStackTrace(ex) + "\n\n" + "Object state: "
			// + this.toString() + "\n\n";
					+ ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE, true, true) + "\n\n";
			mailUtil.sendSecureEmail(ADMIN_EMAIL, ADMIN_EMAIL, null, null, "Heartbeat datavis threw an error!", msg.replaceAll("(\r\n|\n)", "<br />"));
		} catch (Exception innerEx) {
			LOGGER.severe("FATAL ERROR: Could not email exception. Details: " + ex.getMessage());
			System.exit(1); // Exit program.
		}
	}
}
