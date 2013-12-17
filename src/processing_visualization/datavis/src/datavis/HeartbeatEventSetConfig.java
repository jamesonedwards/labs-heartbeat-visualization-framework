package datavis;

public class HeartbeatEventSetConfig {
	// TODO: Add more color patterns.
	public static final int[][] DOT_COLORS = new int[][] { new int[] { 0, 0, 255 }, new int[] { 255, 0, 0 }, new int[] { 0, 255, 0 }, new int[] { 100, 150, 200 } };

	private String eventKey;
	private int[] rgbValues;
	private boolean enabled;
	
	public void setEventKey(String eventKey) {
		this.eventKey = eventKey;
	}

	public String getEventKey() {
		return this.eventKey;
	}
	
	public void setRgbValues(int[] rgbValues) {
		this.rgbValues = rgbValues;
	}

	public int[] getRgbValues() {
		return this.rgbValues;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean getEnabled() {
		return this.enabled;
	}
	
	public HeartbeatEventSetConfig() {
		
	}

	public HeartbeatEventSetConfig(String eventKey, int[] rgbValues, boolean enabled) {
		this.eventKey = eventKey;
		this.rgbValues = rgbValues;
		this.enabled = enabled;
	}
	
	public void toggleEnabled() {
		enabled = !enabled;
	}
}
