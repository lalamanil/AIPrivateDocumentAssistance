package com.document.personal.assistance.model;
/**
@author ANIL LALAM
**/
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenerativeModelResponseModel {

	@JsonProperty("rankedDocuments")
	private List<RankedDocumentsModel> rankedDocuments;

	public List<RankedDocumentsModel> getRankedDocuments() {
		return rankedDocuments;
	}

	public void setRankedDocuments(List<RankedDocumentsModel> rankedDocuments) {
		this.rankedDocuments = rankedDocuments;
	}

}
