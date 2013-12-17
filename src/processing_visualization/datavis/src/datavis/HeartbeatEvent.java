/**
 * This is a data structure that represents events in the heartbeat datasets.
 * 
 * @author jameson.edwards
 * 
 */
package datavis;

import com.google.gson.*;
import util.*;

public class HeartbeatEvent {
	private String event_key;
	private int event_timestamp;
	private String event_datetime;
	private String event_value;
	private String[] event_tags;
	private JsonElement raw_data;
	private Dot innerDot, outerDot;
	// HACK: This value is only used for thw Twitter case.
	private float mappedTimestamp;

	public void setEventKey(String event_key) {
		this.event_key = event_key;
	}

	public String getEventKey() {
		return this.event_key;
	}

	public void setEventTimestamp(int event_timestamp) {
		this.event_timestamp = event_timestamp;
	}

	public int getEventTimestamp() {
		return this.event_timestamp;
	}

	public void setEventDatetime(String event_datetime) {
		this.event_datetime = event_datetime;
	}

	public String getEventDatetime() {
		return this.event_datetime;
	}

	public void setEventValue(String event_value) {
		this.event_value = event_value;
	}

	public String getEventValue() {
		return this.event_value;
	}

	public String[] getEventTags() {
		return event_tags;
	}

	public void setEventTags(String[] event_tags) {
		this.event_tags = event_tags;
	}

	public JsonElement getRawData() {
		return raw_data;
	}

	public Dot getInnerDot() {
		return innerDot;
	}

	public void setInnerDot(Dot dot) {
		this.innerDot = dot;
	}

	public Dot getOuterDot() {
		return outerDot;
	}

	public void setOuterDot(Dot dot) {
		this.outerDot = dot;
	}

	public float getMappedTimestamp() {
		return mappedTimestamp;
	}

	public void setMappedTimestamp(float mappedTimestamp) {
		this.mappedTimestamp = mappedTimestamp;
	}

	public String toString() {
		return String.format("HeartbeatEvent[event_key: %s, event_datetime: %s]", event_key, event_datetime);
	}
}
