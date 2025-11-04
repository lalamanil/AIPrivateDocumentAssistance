package com.document.personal.assistance.model;
/**
@author ANIL LALAM
**/
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonPropertyOrder({ "statusCode", "message", "path", "timeStamp" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrivateDocumentErrorModel {

	@Schema(description = "HTTP status code representing the error response", example = "400")
	@JsonProperty("statusCode")
	private int statusCode;
	@Schema(description = "Detailed error message explaining the cause of failure", example = "Either the bucket Name or Object Name is invalid.")
	@JsonProperty("message")
	private String message;

	@Schema(description = "The request URI where the error occurred", example = "/event")
	@JsonProperty("path")
	private String path;

	@Schema(description = "Timestamp when the error was generated in ISO-8601 format", example = "2025-10-02T00:23:48.186776Z")
	@JsonProperty("timeStamp")
	private Instant timeStamp;

	public PrivateDocumentErrorModel(int statusCode, String message, String path, Instant timeStamp) {

		this.statusCode = statusCode;
		this.message = message;
		this.path = path;
		this.timeStamp = Instant.now();

	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Instant getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Instant timeStamp) {
		this.timeStamp = timeStamp;
	}

}
