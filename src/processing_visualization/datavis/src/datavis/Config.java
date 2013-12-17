package datavis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

public final class Config {
	private Properties properties;
	public final String SERVICE_URL_ROOT;
	public final String SAVED_IMAGE_DIR;
	public final String ASSET_DIR;
	public final boolean SHOW_FULL_SCREEN;
	public final boolean INCLUDE_RETWEETS;
	public final boolean ENABLE_FLICKR_UPLOAD;
	public final int BACKGROUND_IMAGE_REFRESH_THRESHOLD;
	public final int INACTIVITY_THRESHOLD;
	public final Level LOG_LEVEL;
	public final boolean ERROR_EMAIL_ENABLED;
	public final boolean EMAIL_DEBUG_MODE;
	public final String ADMIN_EMAIL;
	public final String ERROR_EMAIL;

	public Config() {
		// Load config file.
		String path = System.getProperty("user.dir") + File.separator + "datavis.properties";
		try (FileInputStream inputStream = new FileInputStream(path)) {
			properties = new Properties();
			properties.load(inputStream);
		} catch (IOException ex) {
			System.err.println("Cannot load properties file: " + ex.getMessage());
		}

		// Set class members.
		SERVICE_URL_ROOT = properties.getProperty("service.url.root");
		SAVED_IMAGE_DIR = properties.getProperty("saved.image.dir");
		ASSET_DIR = properties.getProperty("asset.dir");
		SHOW_FULL_SCREEN = Boolean.parseBoolean(properties.getProperty("show.full.screen"));
		INCLUDE_RETWEETS = Boolean.parseBoolean(properties.getProperty("include.retweets"));
		ENABLE_FLICKR_UPLOAD = Boolean.parseBoolean(properties.getProperty("enable.flickr.upload"));
		BACKGROUND_IMAGE_REFRESH_THRESHOLD = Integer.parseInt(properties.getProperty("background.image.refresh.threshold"));
		INACTIVITY_THRESHOLD = Integer.parseInt(properties.getProperty("inactivity.threshold"));
		LOG_LEVEL = Level.parse(properties.getProperty("log.level"));
		ERROR_EMAIL_ENABLED = Boolean.parseBoolean(properties.getProperty("error.email.enabled"));
		EMAIL_DEBUG_MODE = Boolean.parseBoolean(properties.getProperty("email.debug.mode"));
		ADMIN_EMAIL = properties.getProperty("admin.email");
		ERROR_EMAIL = properties.getProperty("error.email");
	}
}
