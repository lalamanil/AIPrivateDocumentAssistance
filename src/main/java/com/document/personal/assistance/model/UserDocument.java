package com.document.personal.assistance.model;
/**
@author ANIL LALAM
**/
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonPropertyOrder({ "name", "fullName", "mimeType", "signedUrl", "summaryText", "audiosignedUrl" })
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Represents a user's uploaded document stored in GCS.")
public class UserDocument {

	@JsonProperty("name")
	@Schema(description = "Name of the object stored in GCS bucket", example = "AI-PersonalDocument.pdf")
	private String name;

	@JsonProperty("fullName")
	@Schema(description = "Full Name of the object stored in GCS bucket", example = "lalamanilbabu@gmail.com/AI-PersonalDocument.pdf")
	private String fullName;

	@JsonProperty("mimeType")
	@Schema(description = "content-type of the object stored in GCS bucket", example = "application/pdf")
	private String mimeType;
	@JsonProperty("signedUrl")
	@Schema(description = "Signed URL to access the object securely via browser.")
	private String signedUrl;

	@JsonProperty("summaryText")
	@Schema(description = "summary of document")
	private String summaryText;

	@JsonProperty("audiosignedUrl")
	@Schema(description = "Signed URL to access the audio object securely via browser.")
	private String audiosignedUrl;

	public UserDocument() {

	}

	public UserDocument(String name, String fullName, String mimeType, String signedUrl, String summaryText,
			String audiosignedUrl) {
		this.name = name;
		this.fullName = fullName;
		this.mimeType = mimeType;
		this.signedUrl = signedUrl;
		this.summaryText = summaryText;
		this.audiosignedUrl = audiosignedUrl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getSignedUrl() {
		return signedUrl;
	}

	public void setSignedUrl(String signedUrl) {
		this.signedUrl = signedUrl;
	}

	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}

	public String getAudiosignedUrl() {
		return audiosignedUrl;
	}

	public void setAudiosignedUrl(String audiosignedUrl) {
		this.audiosignedUrl = audiosignedUrl;
	}

}
