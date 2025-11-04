package com.document.personal.assistance.model;
/**
@author ANIL LALAM
**/
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "userId", "userPromptForDocSearch" })
public class VectorSearchRequestModel {

	@JsonProperty("userId")
	@Schema(description = "Identifier of the user who uploaded documents to the GCS bucket.", example = "lalamanilbabu@gmail.com")
	@NotBlank(message = "userId is required.It identifies the user associated with uploaded documents in the GCS bucket")
	private String userId;

	@JsonProperty("userPromptForDocSearch")
	@Schema(description = "Natural language query provided by the user to retrieve relevant documents via semantic search.", example = "Please provide the documents related to immigration")
	@NotBlank(message = "userPromptForDocSearch is required. Provide a natural language prompt to retrieve relevant documents.")
	private String userPromptForDocSearch;

	@JsonProperty("relevanceCutoff")
	@Schema(description = "relevanceCutoff threshold that range between 0-1 to retrieve relevant documents in results", example = "0.4")
	private float relevanceCutoff;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserPromptForDocSearch() {
		return userPromptForDocSearch;
	}

	public void setUserPromptForDocSearch(String userPromptForDocSearch) {
		this.userPromptForDocSearch = userPromptForDocSearch;
	}

	public float getRelevanceCutoff() {
		return relevanceCutoff;
	}

	public void setRelevanceCutoff(float relevanceCutoff) {
		this.relevanceCutoff = relevanceCutoff;
	}

}
