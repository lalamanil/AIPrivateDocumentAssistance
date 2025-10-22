package com.document.personal.assistance.service;

import java.util.List;

import com.document.personal.assistance.model.UserDocument;
import com.document.personal.assistance.model.VectorSearchResultsModel;

public interface EmbeddingService {

	public void embedthefiledropedtoGCSBucket(String bucketName, String objectName);

	public List<VectorSearchResultsModel> searchDocumentsForUserPrompt(String userid, String promptString,
			float relevanceCutoff);

	public List<UserDocument> getListofDocumentsForUser(String userid);

}
