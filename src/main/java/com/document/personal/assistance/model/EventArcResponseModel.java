package com.document.personal.assistance.model;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonPropertyOrder({ "status", "timestamp" })
public class EventArcResponseModel {

	@JsonProperty("status")
	@Schema(description = "Status message indicating the outcome of the request", example = "Event Processed.")
	private String status;

	@JsonProperty("timestamp")
	@Schema(description = "Timestamp when the success was generated,formatted in ISO-8601", example = "2025-10-02T00:23:48.186776Z")
	private Instant timestamp;

	public EventArcResponseModel(String status, Instant timestamp) {
		this.status = status;
		this.timestamp = timestamp;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

}
