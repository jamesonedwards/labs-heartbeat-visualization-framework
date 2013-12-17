package datavis;

import java.awt.Color;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.colorchooser.AbstractColorChooserPanel;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import controlP5.Button;
import controlP5.CColor;
import controlP5.ControlP5;
import controlP5.Textfield;

import util.*;

public class DatavisV3 extends PApplet {
	// Load the Config class.
	private final static Config config = new Config();
	
	// Global constants:
	private static final String DATA_URL = config.SERVICE_URL_ROOT + "viewtwitterevents/?hashtag=";
	private static final String IMAGE_UPLOAD_URL = config.SERVICE_URL_ROOT + "_upload_flickr_image/";
	private static final int CENTER_X_OFFSET = 0;
	private static final int CENTER_Y_OFFSET = -15;
	private static final int DOT_RADIUS_SCALE_MIN = 1;
	private static final int DOT_RADIUS_SCALE_MAX = 100;
	// 50% transparency.
	private static final float DOT_OUTER_ALPHA = ShapeUtil.DEFAULT_ALPHA_MAX / 2;
	// How much bigger the printable image should be.
	private static final float SAVED_IMAGE_RATIO = 3;
	// The format that the printable image should be saved in.
	private static final String SAVED_IMAGE_FORMAT = ".png";
	
	// Global theme parameters:
	// Theme: "red", "blue", "tan".
	private static final String THEME = "tan";
	private static int[] backgroundRgb;
	private static int[] CENTER_HASHTAG_COLOR;
	private static int[] CENTER_DATE_COLOR;

	// Global colors:
	private static final int[] TOOLTIP_RGB = new int[] { 240, 240, 240 };
	private static final int[] KEY_RECT_RGB = new int[] { 240, 240, 240 };
	private static final int[] TEXT_RGB = new int[] { 0, 102, 153 };

	// Logging:
	private final static Logger LOGGER = Logger.getLogger("HeartbeatEventLogger");

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
	private static boolean formSubmitted = false;
	private static ControlP5 cp5;
	private static Textfield cp5HashtagField;
	private static Textfield cp5UserEmailField;
	private static Button cp5SubmitButton;
	private static Button cp5SendButton;
	private static Button cp5HashButton;
	private static Button cp5ColorPickerButton;
	private static PImage hashtagFieldButton;
	private static PImage userEmailFieldButton;
	private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String hashtag;
	private static JColorChooser jColorChooser;
	private static PImage labsLogo;
	private static boolean displayLabsLogo;
	private static float labsLogoX;
	private static float labsLogoY;
	private static float inputFieldY;
	private static float inputButtonY;
	private static float hashSendButtonSpacing;
	private static float hashtagFieldColorChooserSpacing;
	private static PImage loadingImage;
	private static boolean displayLoadingImage;
	private static int backgroundImageRefreshCounter;
	private static int inactivityCounter = 0;
	private static PImage currentBackgroundImage;
	private static boolean showImageGallery;
	private static boolean displayHashtagFieldImage;
	private static boolean displayUserEmailFieldImage;

	// Global email config:
	private static final String SMTP_HOST_NAME = "smtp.sendgrid.net";
	private static final String SMTP_AUTH_USER = "sg@mcgarrybowen.com";
	private static final String SMTP_AUTH_PWD = "W6!filVq";
	private static final String IMAGE_EMAIL_SUBJECT = "Your data vis image is ready!";
	private static MailUtil.Config mailConfig;
	private static MailUtil mailUtil;

	// TODO: Image carousel.
	//private static Fader imageFader;
	// Smaller = faster animation, larger = slower animation.
	//private static final float IMAGE_CAROUSEL_SPEED = 50.0f;
	// Carousel of images, as an array.
	private static PImage[] imageCarousel;
	// Index of start image in carousel.
	//private static int imageCarouselShowing;
	//private static int frameCnt = 0;
	
	// HACK: Get this PApplet to run from command line.
	public static void main(String[] args) {
        PApplet.main(DatavisV3.class.getName());
    }
	
	private static void initializeConfig() {
		
	}
	
	public void setup() {
		// This must be the first line of code in setup():
		//size(MAIN_WINDOW_WIDTH, MAIN_WINDOW_HEIGHT, P2D);
		//size(MAIN_WINDOW_WIDTH, MAIN_WINDOW_HEIGHT, OPENGL);
		//size(MAIN_WINDOW_WIDTH, MAIN_WINDOW_HEIGHT);
		size(displayWidth, displayHeight);

		// Setup logging.
		LOGGER.setLevel(config.LOG_LEVEL);
		LOGGER.addHandler(new ConsoleHandler());

		try {
			// Setup email util (in a separate try-catch block because error
			// handling is different if mail fails.
			mailConfig = new MailUtil.Config(SMTP_HOST_NAME, SMTP_AUTH_USER, SMTP_AUTH_PWD, config.EMAIL_DEBUG_MODE);
			mailUtil = new MailUtil(mailConfig);
		} catch (Exception ex) {
			LOGGER.severe(ex.getMessage());
			System.exit(1); // Exit program.
		}

		try {
			// Choose theme.
			switch (THEME) {
			case "red":
				backgroundRgb = new int[] { 255, 0, 0 };
				CENTER_HASHTAG_COLOR = new int[] { 51, 51, 51 };
				CENTER_DATE_COLOR = new int[] { 187, 184, 177 };
				break;
			case "blue":
				backgroundRgb = new int[] { 20, 139, 199 };
				CENTER_HASHTAG_COLOR = new int[] { 51, 51, 51 };
				CENTER_DATE_COLOR = new int[] { 187, 184, 177 };
				break;
			case "tan":
				backgroundRgb = new int[] { 238, 234, 227 };
				CENTER_HASHTAG_COLOR = new int[] { 51, 51, 51 };
				CENTER_DATE_COLOR = new int[] { 204, 204, 204 };
				break;
			}

			// Set up Applet.
			// Window is resizable only if not in full screen mode.
			findFrame().setResizable(!config.SHOW_FULL_SCREEN);
			heartbeatEventGroups = new Hashtable<String, List<HeartbeatEvent>>();
			dotArcs = new ArrayList<HeartbeatEvent[]>();
			cp5 = new ControlP5(this);
			backgroundImageRefreshCounter = 0;
			
			// Load (but don't display) the LABS logo.
			labsLogo = loadImage(config.ASSET_DIR + "labsLogo.png");
			// Shrink the logo by 50%, using zero as the height param to maintain aspect ratio.
			labsLogo.resize(labsLogo.width / 2, 0);
			displayLabsLogo = false;

			// Load (but don't display) the "loading" image.
			loadingImage = loadImage(config.ASSET_DIR + "loadingStatic.png");
			displayLoadingImage = false;
			
			// Run a few calculations.
			setConfig();
			
			// TODO: Finish image carousel: http://www.openprocessing.org/sketch/13104
			// Set up the image carousel.
			//imageFader = new CrossFade(this);

			// TODO: This should come from Flickr rather than local file system.
			File[] bgImagefiles = new File(config.SAVED_IMAGE_DIR + "screenshots").listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(SAVED_IMAGE_FORMAT);
				}
			});
			ArrayList<PImage> tmpList = new ArrayList<PImage>(); 
			imageCarousel = new PImage[bgImagefiles.length];
			int imgBorderHack = 10;
			for (final File fileEntry : bgImagefiles) {
				if (fileEntry.isFile()) {
					PImage tmpImg = loadImage(fileEntry.getAbsolutePath());
					if (tmpImg == null)
						throw new Exception("In setup(), the PImage for page " + fileEntry.getAbsolutePath() + " was null for some reason");
					tmpImg.resize(width + imgBorderHack, height + imgBorderHack);
					tmpList.add(tmpImg);
				}
			}			
			imageCarousel = tmpList.toArray(new PImage[tmpList.size()]);
			tmpList = null;
			
			//  TODO: Carousel stuff
			//int carouselNext = (imageCarouselShowing + 1) % imageCarousel.length;
			//imageFader.setImages(imageCarousel[imageCarouselShowing], imageCarousel[carouselNext]);
			//imageCarouselShowing = carouselNext;
			//imageFader.startFade();

			// Set the default background image.
			currentBackgroundImage = imageCarousel[0];
			
			// Set up input controls.
			PImage goButton = loadImage(config.ASSET_DIR + "goButton.png");
			PImage goButtonPressed = loadImage(config.ASSET_DIR + "goPressed.png");
			PImage sendButton = loadImage(config.ASSET_DIR + "sendButton.png");
			PImage sendButtonPressed = loadImage(config.ASSET_DIR + "sendPressed.png");
			PImage hashButton = loadImage(config.ASSET_DIR + "hashButton.png");
			PImage hashButtonPressed = loadImage(config.ASSET_DIR + "hashPressed.png");
			PImage colorPickerButton = loadImage(config.ASSET_DIR + "colorTri.png");

			// Nudge the positioning around...
			int textFieldWidthOffset = 57;
			int textFieldHeightOffset = 6;
			int textFieldOffsetX = 5;
			
			hashtagFieldButton = loadImage(config.ASSET_DIR + "enterHashtagField.png");
			userEmailFieldButton = loadImage(config.ASSET_DIR + "enterEmailField.png");
			
			int hashtagFieldWidth = hashtagFieldButton.width - textFieldWidthOffset;
			int hashtagFieldHeight = hashtagFieldButton.height - textFieldHeightOffset;
			int userEmailFieldWidth = userEmailFieldButton.width - textFieldWidthOffset;
			int userEmailFieldHeight = userEmailFieldButton.height - textFieldHeightOffset;

			inputFieldY = centerY + radius + 150;
			inputButtonY = inputFieldY + hashtagFieldHeight + 20;
			hashSendButtonSpacing = 10;
			hashtagFieldColorChooserSpacing = 10;

			String hashtagFieldLabelText = "Enter Hashtag";
			String hashtagFieldDefaultText = "#";
			String userEmailFieldLabelText = "Enter Email Address";
			
			cp5HashtagField = cp5.addTextfield("hashtagField")
					.setPosition(centerX - hashtagFieldWidth - hashtagFieldColorChooserSpacing / 2 - textFieldOffsetX, inputFieldY + textFieldHeightOffset / 2)
					.setSize(hashtagFieldWidth, hashtagFieldHeight).setFocus(true).setColor(color(CENTER_HASHTAG_COLOR[0], CENTER_HASHTAG_COLOR[1], CENTER_HASHTAG_COLOR[2])).setAutoClear(true)
					.keepFocus(false).setLabelVisible(true).setCaptionLabel(hashtagFieldLabelText).setColorCursor(color(CENTER_HASHTAG_COLOR[0], CENTER_HASHTAG_COLOR[1], CENTER_HASHTAG_COLOR[2]))
					.setColorCaptionLabel(color(CENTER_HASHTAG_COLOR[0], CENTER_HASHTAG_COLOR[1], CENTER_HASHTAG_COLOR[2])).setText(hashtagFieldDefaultText);
			updateTextFieldColors(cp5HashtagField);
			
			cp5UserEmailField = cp5.addTextfield("userEmailField")
					.setPosition(centerX - userEmailFieldWidth / 2 + textFieldWidthOffset / 2 - textFieldOffsetX, inputFieldY + textFieldHeightOffset / 2)
					.setSize(userEmailFieldWidth, userEmailFieldHeight).setFocus(false).setColor(color(CENTER_HASHTAG_COLOR[0], CENTER_HASHTAG_COLOR[1], CENTER_HASHTAG_COLOR[2])).setAutoClear(true)
					.keepFocus(false).setLabelVisible(true).setCaptionLabel(userEmailFieldLabelText).setColorCursor(color(CENTER_HASHTAG_COLOR[0], CENTER_HASHTAG_COLOR[1], CENTER_HASHTAG_COLOR[2]))
					.setColorCaptionLabel(color(CENTER_HASHTAG_COLOR[0], CENTER_HASHTAG_COLOR[1], CENTER_HASHTAG_COLOR[2]));
			updateTextFieldColors(cp5UserEmailField);

			cp5SubmitButton = cp5.addButton("submitButton").setPosition(centerX - goButton.width / 2, inputButtonY).setSize(goButton.width, goButton.height)
					.setImages(goButton, goButton, goButtonPressed).updateSize();

			cp5SendButton = cp5.addButton("sendButton").setPosition(centerX + hashSendButtonSpacing / 2, inputButtonY).setSize(sendButton.width, sendButton.height)
					.setImages(sendButton, sendButton, sendButtonPressed).updateSize();

			cp5HashButton = cp5.addButton("hashButton").setPosition(centerX - hashButton.width - hashSendButtonSpacing / 2, inputButtonY).setSize(hashButton.width, hashButton.height)
					.setImages(hashButton, hashButton, hashButtonPressed).updateSize();

			cp5ColorPickerButton = cp5.addButton("colorPickerButton").setPosition(centerX + hashtagFieldColorChooserSpacing / 2, inputFieldY)
					.setSize(colorPickerButton.width, colorPickerButton.height).setImages(colorPickerButton, colorPickerButton, colorPickerButton).updateSize();

			// Set initial UI state.
			setUiStateShowGallery();
			
			// Get date range.
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("EDT"));
			// End: Today at midnight.
			endDate = simpleDateFormat.parse(simpleDateFormat.format(cal.getTime()));
			// Start: Yesterday at midnight.
			cal.add(Calendar.DAY_OF_YEAR, -1);
			startDate = simpleDateFormat.parse(simpleDateFormat.format(cal.getTime()));
			LOGGER.info("Date range set to: " + startDate.toString() + " - " + endDate.toString());
		} catch (Exception ex) {
			LOGGER.severe(ex.getMessage());
			emailError(ex);
		}
	}

	/**
	 * Process the submit button click event.
	 * 
	 * @param theValue
	 */
	public void submitButton(int theValue) {
		LOGGER.info("A button event from submitButton: " + theValue);

		// Set the hashtag based on the textfield value.
		hashtag = cp5HashtagField.getText();

		// Signal to the rest of the applet that the user has submitted the
		// form.
		if (hashtag.length() > 2) {
			// Open dialog box to notify user that processing is underway.
			JOptionPane pane = new JOptionPane("We are generating your image. Please stand by.");
			JDialog dialog = pane.createDialog(this, "Processing...");
			dialog.setModal(false);
			dialog.setVisible(true);
			
			// Get data.
			refreshData();

			if (allHeartbeatEvents == null || allHeartbeatEvents.getAllHeartbeatEvents() == null || allHeartbeatEvents.getAllHeartbeatEvents().size() == 0) {
				LOGGER.info("No data found for this hashtag and date range.");
				// TODO: Eventually, remove this line:
				emailError(new Exception("No events found for this hashtag and date range."));
			}

			// Signal that the form has been submitted.
			formSubmitted = true;
			
			// Save a screenshot and upload it to Flickr.
			saveAndUploadScreenshot();
			
			// Switch to "send" state.
			setUiStateSend();
			
			// Close dialog box.
			dialog.setVisible(false);
		}
	}

	/**
	 * Process the send button click event.
	 * 
	 * @param theValue
	 */
	public void sendButton(int theValue) {
		LOGGER.info("A button event from sendButton: " + theValue);

		// TODO: Should include proper email validation.
		if (cp5UserEmailField.getText().length() >= 5) {
			if (config.ENABLE_FLICKR_UPLOAD) {
				try {
					if (flickrImageUrl.length() <= 0) {
						throw new Exception("Cannot send image before uploading it to Flickr!");
					}

					if (cp5UserEmailField.getText().length() > 0) {
						// Send an email to the user containing the Flickr link.
						emailImageToUser();
					}
				} catch (Exception ex) {
					LOGGER.severe(ex.getMessage());
					emailError(ex);
				}
			} else {
				LOGGER.info("IMAGE UPLOAD AND IMAGE EMAIL ARE DISABLED!");
			}

			setUiStateThankYou();
		}
	}

	/**
	 * Process the hash button click event.
	 * 
	 * @param theValue
	 */
	public void hashButton(int theValue) {
		LOGGER.info("A button event from hashButton: " + theValue);
		setUiStateShowGallery();
		
		// Hack: After this gets called, mouseClicked() gets called and
		// immediately unsets the state. To workaround this, set the inactivity
		// counter to -1.
		inactivityCounter = -1;
	}

	/**
	 * Process the hash button click event.
	 * 
	 * @param theValue
	 */
	public void colorPickerButton(int theValue) {
		LOGGER.info("A button event from colorPickerButton: " + theValue);

		// Configure a custom color chooser.
		jColorChooser = new JColorChooser();
		jColorChooser.setLocation(floor(cp5ColorPickerButton.getPosition().x + 500), floor(cp5ColorPickerButton.getPosition().y));

		// Remove all by the "RGB" panel.
		AbstractColorChooserPanel[] panels = jColorChooser.getChooserPanels();
		for (AbstractColorChooserPanel accp : panels) {
			if (!accp.getDisplayName().equals("RGB")) {
				jColorChooser.removeChooserPanel(accp);
			}
		}

		// Remove the "preview" panel.
		jColorChooser.setPreviewPanel(new JPanel());

		// Show the dialog box.
		JDialog dialog = JColorChooser.createDialog(this, "Choose a Background Color", true, jColorChooser, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color color = jColorChooser.getColor();
				if (color != null) {
					backgroundRgb = new int[] { color.getRed(), color.getGreen(), color.getBlue() };
				}

			}
		}, null);
		dialog.setVisible(true);
		
		// Set new colors for textfields.
		updateTextFieldColors(cp5HashtagField);
		updateTextFieldColors(cp5UserEmailField);
	}
	
	private void updateTextFieldColors(controlP5.Textfield field) {
		int newBgColor = color(backgroundRgb[0], backgroundRgb[1], backgroundRgb[2]);
		field.setColorActive(newBgColor).setColorBackground(newBgColor).setColorForeground(newBgColor).update();
	}

	public void mouseClicked() {
		// HACK: Don't immediately exit gallery one hash button click.
		if (inactivityCounter >= 0) {
			if (showImageGallery)
				setUiStateStart();

			// Reset the inactivity counter.
			inactivityCounter = 0;
		}
	}
	
	public void keyPressed() {
		if (showImageGallery)
			setUiStateStart();

		// Reset the inactivity counter.
		inactivityCounter = 0;		
	}
	
	public boolean sketchFullScreen() {
		return config.SHOW_FULL_SCREEN;
	}
	
	public void draw() {
		setConfig();
		
		// If we hit the threshold of inactivity, return to image gallery state.
		if (inactivityCounter >= config.INACTIVITY_THRESHOLD) {
			setUiStateShowGallery();

			// Reset the inactivity counter.
			inactivityCounter = 0;
		}
		
		if (showImageGallery) {
			// Load existing images (cycle through them).
			if (backgroundImageRefreshCounter >= config.BACKGROUND_IMAGE_REFRESH_THRESHOLD) {
				// Change the BG image.
				Random rand = new Random();
				int imgIndex = rand.nextInt(imageCarousel.length);
				currentBackgroundImage = imageCarousel[imgIndex];
				// Reset counter.
				backgroundImageRefreshCounter = 0;
			} else {
				backgroundImageRefreshCounter++;
			}

			// HACK: The background() function in Processing has a bug, so set as full-size background image.
			image(currentBackgroundImage, 0, 0);
			
			// Reset the inactivity counter (to work with "show gallery after hash button click" hack).
			inactivityCounter = 0;
		} else {
			// Increment the counter only when the gallery isn't being shown. 
			inactivityCounter++;
		}
		
		// Elements to conditionally draw.
		if (displayHashtagFieldImage)
			image(hashtagFieldButton, centerX - hashtagFieldButton.width - hashtagFieldColorChooserSpacing / 2, inputFieldY);
		if (displayUserEmailFieldImage)
			image(userEmailFieldButton, centerX - hashtagFieldButton.width / 2, inputFieldY);
		if (displayLabsLogo)
			image(labsLogo, labsLogoX, labsLogoY);
		if (displayLoadingImage)
			image(loadingImage, centerX - loadingImage.width / 2, centerY - loadingImage.height / 2);
		
		// TODO: Carousel stuff: This code seems to cause the JRE to crash!
		/*try {
			if (frameCnt <= IMAGE_CAROUSEL_SPEED) {
				// Show next frame of the animation
				imageFader.animateFade(frameCount / IMAGE_CAROUSEL_SPEED);
			} else {
				// Show end image
				imageFader.endFade();

				// Reset counter.
				frameCnt = 0;
			}

			frameCnt++;

			// Draw current state of transition effect
			imageFader.draw();
		} catch (Exception ex) {
			LOGGER.info("BAD FRAME: " + ex.getMessage());
		}*/

		if (formSubmitted) {
			// This is a hack since there is only one data set, but whatever.
			if (allHeartbeatEvents != null && allHeartbeatEvents.getAllHeartbeatEvents() != null && allHeartbeatEvents.getAllHeartbeatEvents().size() > 0) {
				for (int i = 0; i < allHeartbeatEvents.getAllowedEventKeys().length; i++) {
					String key = allHeartbeatEvents.getAllowedEventKeys()[i];
					if (heartbeatEventGroups.get(key) != null && heartbeatEventGroups.get(key).size() > 0) {
						renderEventSet(heartbeatEventGroups.get(key));
					}
				}
			}

			// Check if mouse is hovered over a dot.
			// checkForDotHover();

			// Draw the instructions for the visualization.
			// drawDataSetKey();

			// Draw the center text.
			drawCenterText();
		}
	}

	private void saveAndUploadScreenshot() {
		LOGGER.info("saveAndUploadScreenshot() called");
		String screenshotName = config.SAVED_IMAGE_DIR + "screenshots" + File.separator + hashtag + "_" + simpleDateFormat.format(startDate) + "_" + simpleDateFormat.format(endDate) + "_" + THEME + SAVED_IMAGE_FORMAT;
		LOGGER.info("screenshotName set to " + screenshotName);
		PGraphics hires = createGraphics(floor(width * SAVED_IMAGE_RATIO), floor(height * SAVED_IMAGE_RATIO), JAVA2D);
		LOGGER.info("PGraphics created");
		
		// Hide controls, etc.
		setUiStatePrintView();
		
		// Start recording.
		beginRecord(hires);
		LOGGER.info("beginRecord(hires) called");
		hires.scale(SAVED_IMAGE_RATIO);
		LOGGER.info("hires.scale(SAVED_IMAGE_RATIO) called");
		draw();
		LOGGER.info("draw() called");
		
		// End recording.
		endRecord();
		LOGGER.info("endRecord() called");
		hires.save(screenshotName);
		LOGGER.info("hires.save(screenshotName) called");
		
		// Alert user that the image has been saved.
		LOGGER.info("Screenshot saved to " + screenshotName);

		if (config.ENABLE_FLICKR_UPLOAD) {
			try {
				// Upload the file to Flickr.
				String response = UrlUtil.uploadFile(screenshotName, IMAGE_UPLOAD_URL);

				// Extract the flickr ID from the response and include that in
				// the email to the user.
				FlickrImageResponse flickrImageResponse = new Gson().fromJson(response, FlickrImageResponse.class);
				flickrImageUrl = flickrImageResponse.getImageUrl();
			} catch (Exception ex) {
				LOGGER.severe(ex.getMessage());
				emailError(ex);
			}
		} else {
			LOGGER.info("IMAGE UPLOAD AND IMAGE EMAIL ARE DISABLED!");
		}
	}

	private void setConfig() {
		// Redraw background.
		background(backgroundRgb[0], backgroundRgb[1], backgroundRgb[2]);

		// Set hidden circle values.
		centerX = width / 2 + CENTER_X_OFFSET;
		centerY = height / 2 + CENTER_Y_OFFSET;
		radius = width / 9;
		
		// Set the position of the LABS logo.
		labsLogoX = centerX - labsLogo.width / 2;
		labsLogoY = centerY + 20;
	}

	/**
	 * Get the current frame. (A hack for Eclipse from
	 * https://forum.processing.org/topic/trying-to-use-processing-in-eclipse).
	 * 
	 * @return
	 */
	public Frame findFrame() {
		Container f = this.getParent();
		while (!(f instanceof Frame) && f != null)
			f = f.getParent();
		return (Frame) f;
	}

	private int getRetweetWorth(JsonObject tweet) {
		// float multiplier = 500.0f;
		float multiplier = 1.0f;
		return floor(tweet.get("retweet_count").getAsInt() * multiplier);
	}

	private void refreshData() {
		LOGGER.info("Refreshing data.");
		try {
			// Get all heartbeat events for the given date range.
			String url = DATA_URL + URLEncoder.encode(hashtag, "UTF-8");
			url += "&date_start=" + simpleDateFormat.format(startDate) + "&date_end=" + simpleDateFormat.format(endDate);
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
							} else if (config.INCLUDE_RETWEETS && retweetedStatusIdStr1 != null && retweetedStatusIdStr1.equals(idStr2)) {
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
		// float mappedTS = map(event.getEventTimestamp(),
		// allHeartbeatEvents.getTimestampMin(),
		// allHeartbeatEvents.getTimestampMax(), 0, 359);
		// UPDATE: Instead of the max and min for the dataset, use the start and
		// end dates for the search. Note: divide by 1000 to remove the
		// milliseconds.
		// float mappedTS = map(event.getEventTimestamp(),
		// floor(startDate.getTime() / 1000), floor(endDate.getTime() / 1000),
		// 0, 359);
		// UPDATE #2: Undo previous update:
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
			String userEmail = cp5UserEmailField.getText();
			if (userEmail.length() <= 0) // TODO: Validate email address.
				throw new Exception("Invalid email address: " + userEmail);
			String msg = "Here is your data visualization for " + hashtag + ":\n\n<a href=\"" + flickrImageUrl + "\">" + flickrImageUrl + "</a>\n\n";
			mailUtil.sendSecureEmail(userEmail, config.ADMIN_EMAIL, null, config.ADMIN_EMAIL, IMAGE_EMAIL_SUBJECT, msg.replaceAll("(\r\n|\n)", "<br />"));
		} catch (Exception ex) {
			LOGGER.severe("FATAL ERROR: Could not email image to user. Details: " + ex.getMessage());
			System.exit(1); // Exit program.
		}
	}

	private void emailError(Exception ex) {
		try {
			String msg = "Exception details: \nType: " + ex.getClass().toString() + "\nMessage: " + ex.getMessage() + "\nStack trace: " + ExceptionUtils.getStackTrace(ex) + "\n\n" + "Object state: "
					+ ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE, true, true) + "\n\n";
			if (config.ERROR_EMAIL_ENABLED) {
				mailUtil.sendSecureEmail(config.ERROR_EMAIL, config.ERROR_EMAIL, null, null, "Heartbeat datavis threw an error!", msg.replaceAll("(\r\n|\n)", "<br />"));
			} else {
				LOGGER.info("There was an error but email is diabled: " + msg);
			}
		} catch (Exception innerEx) {
			LOGGER.severe("FATAL ERROR: Could not email exception. Details: " + ex.getMessage());
			System.exit(1); // Exit program.
		}
	}

	private void setUiStateShowGallery() {
		cp5HashtagField.clear().setVisible(false);
		cp5UserEmailField.clear().setVisible(false);
		cp5SubmitButton.setVisible(true);
		cp5SendButton.setVisible(false);
		cp5HashButton.setVisible(false);
		cp5ColorPickerButton.setVisible(true);
		displayLoadingImage = false;
		displayHashtagFieldImage = true;
		displayUserEmailFieldImage = false;
		showImageGallery = true;
		formSubmitted = false;
		displayLabsLogo = false;
		
		// Reset the inactivity counter.
		inactivityCounter = 0;
	}

	private void setUiStateStart() {
		cp5HashtagField.setText("#").setVisible(true);
		cp5UserEmailField.clear().setVisible(false);
		cp5SubmitButton.setVisible(true);
		cp5SendButton.setVisible(false);
		cp5HashButton.setVisible(false);
		cp5ColorPickerButton.setVisible(true);
		displayLoadingImage = false;
		displayHashtagFieldImage = true;
		displayUserEmailFieldImage = false;
		showImageGallery = false;
		displayLabsLogo = false;

		// Reset the inactivity counter.
		inactivityCounter = 0;
		
		// Set cursor focus on textfield.
		cp5HashtagField.setFocus(true);
	}

	private void setUiStateLoading() {
		cp5HashtagField.setVisible(false);
		cp5UserEmailField.clear().setVisible(false);
		cp5SubmitButton.setVisible(false);
		cp5SendButton.setVisible(false);
		cp5HashButton.setVisible(false);
		cp5ColorPickerButton.setVisible(false);
		displayLoadingImage = true;
		displayHashtagFieldImage = false;
		displayUserEmailFieldImage = false;
		showImageGallery = false;
		displayLabsLogo = false;

		// Reset the inactivity counter.
		inactivityCounter = 0;
	}

	private void setUiStateSend() {
		cp5HashtagField.setVisible(false);
		cp5UserEmailField.clear().setVisible(true);
		cp5SubmitButton.setVisible(false);
		cp5SendButton.setVisible(true);
		cp5HashButton.setVisible(true);
		cp5ColorPickerButton.setVisible(false);
		displayLoadingImage = false;
		displayHashtagFieldImage = false;
		displayUserEmailFieldImage = true;
		showImageGallery = false;
		displayLabsLogo = false;
		
		// Set the position of the hash button.
		cp5HashButton.setPosition(centerX - cp5HashButton.getWidth() - hashSendButtonSpacing / 2, cp5HashButton.getPosition().y);

		// Reset the inactivity counter.
		inactivityCounter = 0;

		// Set cursor focus on textfield.
		cp5UserEmailField.setFocus(true);
	}

	private void setUiStateThankYou() {
		// TODO: What to do here?
		cp5HashtagField.setVisible(false);
		cp5UserEmailField.clear().setVisible(false);
		cp5SubmitButton.setVisible(false);
		cp5SendButton.setVisible(false);
		cp5HashButton.setVisible(true);
		cp5ColorPickerButton.setVisible(false);
		displayLoadingImage = false;
		displayHashtagFieldImage = false;
		displayUserEmailFieldImage = false;
		showImageGallery = false;
		displayLabsLogo = false;

		// Set the position of the hash button.
		cp5HashButton.setPosition(centerX - cp5HashButton.getWidth() / 2, cp5HashButton.getPosition().y);

		// Reset the inactivity counter.
		inactivityCounter = 0;
	}

	private void setUiStatePrintView() {
		cp5HashtagField.clear().setVisible(false);
		cp5UserEmailField.clear().setVisible(false);
		cp5SubmitButton.setVisible(false);
		cp5SendButton.setVisible(false);
		cp5HashButton.setVisible(false);
		cp5ColorPickerButton.setVisible(false);
		displayLoadingImage = false;
		displayHashtagFieldImage = false;
		displayUserEmailFieldImage = false;
		showImageGallery = false;
		formSubmitted = true;
		displayLabsLogo = true;
	}
}
