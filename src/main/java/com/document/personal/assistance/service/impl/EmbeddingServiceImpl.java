package com.document.personal.assistance.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;
import com.document.personal.assistance.constants.ApplicationConstants;
import com.document.personal.assistance.exception.PrivateDocumentException;
import com.document.personal.assistance.model.UserDocument;
import com.document.personal.assistance.model.VectorSearchResultsModel;
import com.document.personal.assistance.service.EmbeddingService;
import com.document.personal.assistance.utility.BigQueryUtility;
import com.document.personal.assistance.utility.DocumentOCROnlineInferenceUtility;
import com.document.personal.assistance.utility.GeneralUtility;
import com.document.personal.assistance.utility.NotNullEmptyUtility;
import com.document.personal.assistance.utility.PDFSplitterUtility;
import com.document.personal.assistance.utility.StorageUtility;
import com.document.personal.assistance.utility.TextChunkerUtility;
import com.document.personal.assistance.utility.TextEmbeddingWithPredictionServiceUtility;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

	private static final Logger LOGGER = Logger.getLogger(EmbeddingServiceImpl.class.getName());

	@Override
	public void embedthefiledropedtoGCSBucket(String bucketName, String objectName) {
		// TODO Auto-generated method stub
		if (NotNullEmptyUtility.notNullEmptyCheck(bucketName) && NotNullEmptyUtility.notNullEmptyCheck(objectName)) {
			String userid = objectName.split("/")[0];
			// pulling object content from GCS bucket
			Map<String, Object> contentMap = StorageUtility.getObjectFromBucket(bucketName, objectName);
			if (null != contentMap && !contentMap.isEmpty()) {
				byte[] content = contentMap.containsKey("content") ? ((byte[]) contentMap.get("content")) : null;
				String contentType = contentMap.containsKey("contentType") ? (String) contentMap.get("contentType")
						: null;
				if (null != content && null != contentType) {
					String text = null;
					if (ApplicationConstants.MIME_TYPE_PDF.equalsIgnoreCase(contentType)) {
						text = PDFSplitterUtility.getRawTextBySplitingPdfDocOCR(content, contentType);
					} else {
						// Calling Document OCR API (Document AI) for raw text from provided document
						text = DocumentOCROnlineInferenceUtility.processDocument(content, contentType);
					}
					if (NotNullEmptyUtility.notNullEmptyCheck(text)) {
						List<String> chunkList = TextChunkerUtility.chunkText(text, ApplicationConstants.MAX_CHUNK_SIZE,
								ApplicationConstants.OVERLAPPING);
						LOGGER.info("ChunkList size is:" + chunkList.size());
						if (null != chunkList && !chunkList.isEmpty()) {
							// calling text embedded model to generator the vectors for each chuck for
							// semantic search
							List<float[]> vectorList = TextEmbeddingWithPredictionServiceUtility
									.getEmbeddings(chunkList, ApplicationConstants.TASK_TYPE_RETRIEVAL_DOCUMENT);
							if (null != vectorList && !vectorList.isEmpty()) {
								LOGGER.info("vectorList size is:" + vectorList.size());
								int chunkCount = 1;
								List<Map<String, Object>> rowList = new ArrayList<Map<String, Object>>();
								for (int i = 0; i < vectorList.size(); i++) {
									Map<String, Object> row = new HashMap<String, Object>();
									row.put("row_id", objectName + "_chunck" + chunkCount);
									row.put("doc_id", objectName);
									row.put("chunk_id", "chunck" + chunkCount);
									row.put("text", chunkList.get(i));
									row.put("userid", userid);
									row.put("content_type", contentType);
									float[] floatvectors = vectorList.get(i);
									List<Double> emblist = new ArrayList<Double>();
									for (float f : floatvectors) {
										emblist.add((double) f);
									}
									row.put("embedding", emblist);
									rowList.add(row);
									chunkCount++;
								}
								LOGGER.info("Total vector rows trying to insert into Bigquery is:" + rowList.size());

								if (!rowList.isEmpty()) {
									BigQueryUtility.insertRowsBatch(rowList, ApplicationConstants.TABLE_NAME);
								}

							} else {
								LOGGER.info(
										"vectorList from embedding model is null or empty. So not moving forward to store in Bigquery vector db");
							}

						} else {
							LOGGER.info(
									"ChunkList is null or empty. Can not move forward to call embedding model to generate vectors");
						}

					} else {

						LOGGER.info("raw text from Document OCR api is null or empty. So not moving forward.");
					}

				} else {
					LOGGER.info(
							"content and contentType cannot be null. Please check bucket and object is present in GCS bucket");
				}

			} else {
				LOGGER.info("contentMap is null or empty. Have not received from bucket:" + bucketName + " and object:"
						+ objectName + ". Please check bucket and object is present in your project.");
				throw new PrivateDocumentException("Either the bucket Name or Object Name is invalid.", 400);
			}

		} else {
			LOGGER.info("bucketName & objectName  are mandatory feilds. Cannot be null or empty.");

			throw new PrivateDocumentException(
					"bucketName(bucket) and objectName (name) cannot be null or empty in request body", 400);

		}

	}

	@Override
	public List<VectorSearchResultsModel> searchDocumentsForUserPrompt(String userid, String promptString,
			float relevanceCutoff) {
		// TODO Auto-generated method stub
		if (!NotNullEmptyUtility.notNullEmptyCheck(userid)) {
			throw new PrivateDocumentException("userid cannot be null or empty. Please provide valid userid", 400);
		}
		if (!NotNullEmptyUtility.notNullEmptyCheck(promptString)) {
			throw new PrivateDocumentException("promptString cannot be null or empty. Please provide valid userid",
					400);
		}
		List<float[]> vectorlist = TextEmbeddingWithPredictionServiceUtility.getEmbeddings(Arrays.asList(promptString),
				ApplicationConstants.TASK_TYPE_RETRIEVAL_QUERY);
		if (null != vectorlist && !vectorlist.isEmpty()) {
			float[] vector = vectorlist.get(0);
			List<Double> doubleVector = new ArrayList<Double>(vector.length);
			for (float f : vector) {
				doubleVector.add((double) f);
			}
			LOGGER.info("vector for user prompt is: " + doubleVector);
			List<VectorSearchResultsModel> list = BigQueryUtility.vectorSearch(userid,
					ApplicationConstants.TOP_CHUNK_COUNT, doubleVector);
			return GeneralUtility.filterVectorSearchBasedOnRelevenceCutoff(list, relevanceCutoff);
		} else {
			LOGGER.info("vectorList for user prompt is null or empty.");
			throw new PrivateDocumentException("vectorList for user prompt is null or empty.", 500);
		}

	}

	@Override
	public List<UserDocument> getListofDocumentsForUser(String userid) {
		// TODO Auto-generated method stub
		return StorageUtility.getDocumentsOnUserid(userid);
	}

}
