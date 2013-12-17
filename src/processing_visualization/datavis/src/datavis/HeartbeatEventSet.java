package datavis;

import java.util.List;

public class HeartbeatEventSet {
	private List<HeartbeatEvent> heartbeat_events;
	private long timestamp_max;
	private long timestamp_min;
	private String[] allowed_event_keys;
	
	public void setAllHeartbeatEvents(List<HeartbeatEvent> heartbeat_events) {
		this.heartbeat_events = heartbeat_events;
	}

	public List<HeartbeatEvent> getAllHeartbeatEvents() {
		return this.heartbeat_events;
	}
	
	public void setTimestampMax(long timestamp_max) {
		this.timestamp_max = timestamp_max;
	}

	public long getTimestampMax() {
		return this.timestamp_max;
	}
	
	public void setTimestampMin(long timestamp_min) {
		this.timestamp_max = timestamp_min;
	}

	public long getTimestampMin() {
		return this.timestamp_min;
	}
	
	public void setAllowedEventKeys(String[] allowed_event_keys) {
		this.allowed_event_keys = allowed_event_keys;
	}

	public String[] getAllowedEventKeys() {
		return this.allowed_event_keys;
	}
}
