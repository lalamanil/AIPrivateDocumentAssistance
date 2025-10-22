package com.document.personal.assistance.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonPropertyOrder({ "documents", "message", "statusCode" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPromptResponseModel {

	@JsonProperty("documents")
	@Schema(description = "Collection of documents that are relevant to the user's prompt. "
			+ "If no matching documents are found, this list will be empty.")
	private List<VectorSearchResultsModel> documents;

	@JsonProperty("message")
	@Schema(description = "Indicates the result of the search operation. "
			+ "Examples include: 'Relevant documents found' or "
			+ "'No relevant documents found for the given prompt'.", example = "Relevant documents found")
	private String message;

	@JsonProperty("statusCode")
	@Schema(description = "HTTP status code returned by the endpoint. "
			+ "Typically 200 for successful requests.", example = "200")
	private int statusCode;

	public List<VectorSearchResultsModel> getDocuments() {
		return documents;
	}

	public void setDocuments(List<VectorSearchResultsModel> documents) {
		this.documents = documents;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

}
