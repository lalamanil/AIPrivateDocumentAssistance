package com.document.personal.assistance.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "documentId", "distanceList", "numberOfChunks", "signedUrl", "contentType", "createdAt" })
public class VectorSearchResultsModel {

	@JsonProperty("documentId")
	@Schema(description = "Unique identifier of the document stored in the GCS bucket.", example = "lalamanilbabu@gmail.com/Building_Accident_Detection_System_Using_Java_GoogleCloud_VertexAI.pdf")
	private String documentId;

	@JsonProperty("distanceList")
	@Schema(description = "List of cosine distances representing similarity between the user prompt and document chunks. Top 10 closest chunks are provided.", example = "[0.24372218986121652,0.2516626122117682,0.27268671278459344,0.27356756686235606,0.27485940529417463,0.27918230132758837,0.2805830352944597,0.2809073938732316,0.29620080760570033,0.298830454558216]")
	private List<Double> distanceList;

	@JsonProperty("numberOfChunks")
	@Schema(description = "Number of document chunks returned that are closest to the given user prompt.", example = "10")
	private long numberOfChunks;

	@JsonProperty("signedUrl")
	@Schema(description = "Signed URL to access the object securely via browser.")
	private String signedUrl;

	@JsonProperty("contentType")
	@Schema(description = "Content Type of the document stores in GCS bucket.", example = "application/pdf")
	private String contentType;

	@JsonProperty("createdAt")
	@Schema(description = "ISO-8601 formatted timestamp indicating when the search results were generated.", example = "2025-10-02T00:23:48.186776Z")
	private String createdAt;

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public String getSignedUrl() {
		return signedUrl;
	}

	public void setSignedUrl(String signedUrl) {
		this.signedUrl = signedUrl;
	}

	public List<Double> getDistanceList() {
		return distanceList;
	}

	public void setDistanceList(List<Double> distanceList) {
		this.distanceList = distanceList;
	}

	public long getNumberOfChunks() {
		return numberOfChunks;
	}

	public void setNumberOfChunks(long numberOfChunks) {
		this.numberOfChunks = numberOfChunks;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

}
