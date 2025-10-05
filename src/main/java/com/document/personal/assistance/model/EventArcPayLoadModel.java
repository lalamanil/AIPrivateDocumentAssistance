package com.document.personal.assistance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@JsonPropertyOrder({ "kind", "id", "selfLink", "name", "bucket", "generation", "contentType", "storageClass",
		"mediaLink" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventArcPayLoadModel {

	@JsonProperty("kind")
	@Schema(hidden = true)
	private String kind;

	@JsonProperty("id")
	@Schema(hidden = true)
	private String id;

	@JsonProperty("selfLink")
	@Schema(hidden = true)
	private String selfLink;

	@Schema(description = "Name of the document uploaded to the GCS bucket", example = "contract.pdf")
	@JsonProperty("name")
	@NotBlank(message = "Document name is required. Provide the object name stored in the GCS bucket.")
	private String name;

	@Schema(description = "Name of the GCS bucket where the document is stored.", example = "documentassistance")
	@JsonProperty("bucket")
	@NotBlank(message = "Bucket is required.")
	private String bucket;

	@JsonProperty("generation")
	@Schema(hidden = true)
	private String generation;

	@Schema(description = "MIME type of the uploaded document.", example = "application/pdf")
	@JsonProperty("contentType")
	@NotBlank(message = "Document contentType is required.")
	private String contentType;

	@JsonProperty("storageClass")
	@Schema(hidden = true)
	private String storageClass;

	@JsonProperty("mediaLink")
	@Schema(hidden = true)
	private String mediaLink;

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSelfLink() {
		return selfLink;
	}

	public void setSelfLink(String selfLink) {
		this.selfLink = selfLink;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getGeneration() {
		return generation;
	}

	public void setGeneration(String generation) {
		this.generation = generation;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getStorageClass() {
		return storageClass;
	}

	public void setStorageClass(String storageClass) {
		this.storageClass = storageClass;
	}

	public String getMediaLink() {
		return mediaLink;
	}

	public void setMediaLink(String mediaLink) {
		this.mediaLink = mediaLink;
	}

}
