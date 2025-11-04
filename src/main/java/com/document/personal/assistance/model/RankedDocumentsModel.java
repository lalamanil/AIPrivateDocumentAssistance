package com.document.personal.assistance.model;
/**
@author ANIL LALAM
**/
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "id", "relevanceScore", "shortReason" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class RankedDocumentsModel {

	@JsonProperty("id")
	private String id;

	@JsonProperty("relevanceScore")
	private float relevanceScore;

	@JsonProperty("shortReason")
	private String shortReason;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public float getRelevanceScore() {
		return relevanceScore;
	}

	public void setRelevanceScore(float relevanceScore) {
		this.relevanceScore = relevanceScore;
	}

	public String getShortReason() {
		return shortReason;
	}

	public void setShortReason(String shortReason) {
		this.shortReason = shortReason;
	}

}
