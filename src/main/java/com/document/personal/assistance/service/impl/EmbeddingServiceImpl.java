package com.document.personal.assistance.service.impl;
/**
@author ANIL LALAM
**/
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;
import com.document.personal.assistance.constants.ApplicationConstants;
import com.document.personal.assistance.exception.PrivateDocumentException;
import com.document.personal.assistance.model.CandidateDocuments;
import com.document.personal.assistance.model.DocumentEntry;
import com.document.personal.assistance.model.GenerativeModelResponseModel;
import com.document.personal.assistance.model.RankedDocumentsModel;
import com.document.personal.assistance.model.UserDocument;
import com.document.personal.assistance.model.VectorSearchResultsModel;
import com.document.personal.assistance.service.EmbeddingService;
import com.document.personal.assistance.utility.BigQueryUtility;
import com.document.personal.assistance.utility.DocumentOCROnlineInferenceUtility;
import com.document.personal.assistance.utility.FireStoreUtility;
import com.document.personal.assistance.utility.GeneralUtility;
import com.document.personal.assistance.utility.NotNullEmptyUtility;
import com.document.personal.assistance.utility.ObjectMapperSingleton;
import com.document.personal.assistance.utility.PDFSplitterUtility;
import com.document.personal.assistance.utility.StorageUtility;
import com.document.personal.assistance.utility.TextChunkerUtility;
import com.document.personal.assistance.utility.TextEmbeddingWithPredictionServiceUtility;
import com.document.personal.assistance.utility.TextToSpeechUtility;
import com.document.personal.assistance.utility.VertexAIGeminiAPI;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

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

								// Summarizing the raw text using gemini-2.5-flash
								String summaryPrompt = "Summarize this text: " + text;
								String summaryText = VertexAIGeminiAPI.reRankSummarizeDocuments(summaryPrompt);
								if (NotNullEmptyUtility.notNullEmptyCheck(summaryText)) {
									String htmlsupportedSummaryText = GeneralUtility
											.convertBoldToHtmlOrPlainText(summaryText, true);
									String[] useridObject = objectName.split("/");
									Boolean summaryStored = FireStoreUtility.storeSummaryForDocument(userid,
											(useridObject.length > 1 ? useridObject[1] : null), objectName,
											htmlsupportedSummaryText);
									LOGGER.info("summaray stored in FireStore:" + summaryStored);
									if (summaryStored) {
										byte[] audiobytes = TextToSpeechUtility.convertTextToSpeech(
												GeneralUtility.convertBoldToHtmlOrPlainText(summaryText, false),
												ApplicationConstants.TEXT_TO_SPEECH_LANGUAGE_CODE);
										if (null != audiobytes) {
											StorageUtility.writeObjectToBucket(ApplicationConstants.AUDIO_BUCKET,
													objectName, audiobytes, ApplicationConstants.AUDIO_CONTENT_TYPE);
										}

									}

								} else {
									LOGGER.info("summaryText received from Gemini-2.5-flash is null or empty");
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
			String geminiPromptForReranking = buildPromptToGeminiToRankDocuments(promptString, list);
			LOGGER.info("gemini Prompt String for re-ranking:" + geminiPromptForReranking);

			List<VectorSearchResultsModel> listFromGenerativeModel = null;

			if (NotNullEmptyUtility.notNullEmptyCheck(geminiPromptForReranking)) {
				String responseFromGenerativeModel = VertexAIGeminiAPI
						.reRankSummarizeDocuments(geminiPromptForReranking);
				// LOGGER.info("responseFromGenerativeModel:" + responseFromGenerativeModel);
				listFromGenerativeModel = mapResponseFromGenerativeModeltoExistingVectorSearch(
						responseFromGenerativeModel, list);
			}
			if (null != listFromGenerativeModel && !listFromGenerativeModel.isEmpty()) {
				return GeneralUtility.filterVectorsFromGenerativeModelOnRelevenceCutoff(listFromGenerativeModel,
						ApplicationConstants.RELEVANCE_GENERATIVE_MODEL_CUTOFF);
			} else {
				return GeneralUtility.filterVectorSearchBasedOnRelevenceCutoff(list, relevanceCutoff);
			}

		} else {
			LOGGER.info("vectorList for user prompt is null or empty.");
			throw new PrivateDocumentException("vectorList for user prompt is null or empty.", 500);
		}

	}

	public String buildPromptToGeminiToRankDocuments(String promptString, List<VectorSearchResultsModel> list) {

		StringBuilder promptBuilder = new StringBuilder();
		if (null != list && !list.isEmpty()) {
			CandidateDocuments candidateDocuments = getCandidateDocuments(list);
			if (!candidateDocuments.getDocuments().isEmpty()) {
				String candidateDocumentJson = null;
				try {
					candidateDocumentJson = ObjectMapperSingleton.INSTANCE.getObjectMapper()
							.writeValueAsString(candidateDocuments);
					if (null != candidateDocumentJson) {

						promptBuilder.append(
								"You are a document relevance re-ranking model. Your task is to re-rank the following retrieved document chunks based on their semantic relevance to the user query.");
						promptBuilder.append("\r\n");

						promptBuilder.append("User query:");
						promptBuilder.append("\r\n");
						promptBuilder.append(promptString);
						promptBuilder.append("\r\n");

						promptBuilder.append("Candidate documents:");
						promptBuilder.append("\r\n");

						promptBuilder.append(candidateDocumentJson);

						promptBuilder.append("\r\n");

						promptBuilder.append("Instructions:");

						promptBuilder.append("1.Carefully read the query and each document text.");
						promptBuilder.append("\r\n");
						promptBuilder.append("2.Rank all documents from most relevant to least relevant.");
						promptBuilder.append("\r\n");
						promptBuilder.append("3. Provide output in **JSON** format with the following structure:");
						promptBuilder.append("\r\n");

						promptBuilder.append(
								"{“rankedDocuments\": [{\"id\": \"<document id>\",\"relevanceScore\": <float between 0 and 1>,\"shortReason\": \"<brief reason for the score and answer the User query>\" }]}");

						promptBuilder.append("\r\n");
						promptBuilder.append(
								"The higher the relevanceScore, the more semantically relevant the document is to the query.");

					} else {
						LOGGER.info("candidateDocumentJson is null or empty..");
					}

				} catch (JsonProcessingException e) {
					// TODO: handle exception
					LOGGER.info("Exception occured while serializing java object to Json." + e.getMessage());
					e.printStackTrace();
				}

			} else {
				LOGGER.info("Document Entries are null or empty for candidateDocuments");
			}

		} else {
			LOGGER.info("vectorList is null or empty..");
		}

		return promptBuilder.toString();

	}

	public Map<String, VectorSearchResultsModel> getVectorIdMap(List<VectorSearchResultsModel> vectorModels) {

		final Map<String, VectorSearchResultsModel> idvectorMap = new HashMap<String, VectorSearchResultsModel>();
		vectorModels.forEach(vsrm -> {
			idvectorMap.put(vsrm.getDocumentId(), vsrm);
		});
		return idvectorMap;
	}

	public List<VectorSearchResultsModel> mapResponseFromGenerativeModeltoExistingVectorSearch(
			String responseFromGenerativeModel, List<VectorSearchResultsModel> vectorList) {
		final List<VectorSearchResultsModel> mappedVectorsList = new ArrayList<VectorSearchResultsModel>();
		if (NotNullEmptyUtility.notNullEmptyCheck(responseFromGenerativeModel) && null != vectorList
				&& !vectorList.isEmpty()) {
			String cleanJsonresponse = extractPureJson(responseFromGenerativeModel);
			LOGGER.info("cleanJsonresponse::" + cleanJsonresponse);
			try {
				GenerativeModelResponseModel responseModel = ObjectMapperSingleton.INSTANCE.getObjectMapper()
						.readValue(cleanJsonresponse, GenerativeModelResponseModel.class);
				if (null != responseModel && null != responseModel.getRankedDocuments()
						&& !responseModel.getRankedDocuments().isEmpty()) {
					List<RankedDocumentsModel> rankedDocumentsModels = responseModel.getRankedDocuments();
					Map<String, VectorSearchResultsModel> idVectorMap = getVectorIdMap(vectorList);

					rankedDocumentsModels.forEach(rdm -> {
						if (NotNullEmptyUtility.notNullEmptyCheck(rdm.getId())
								&& idVectorMap.containsKey(rdm.getId())) {
							VectorSearchResultsModel vsrm = idVectorMap.get(rdm.getId());
							vsrm.setRelevanceScore(rdm.getRelevanceScore());
							vsrm.setShortReason(rdm.getShortReason());
							mappedVectorsList.add(vsrm);
						}
					});

				}

			} catch (JsonMappingException e) {
				// TODO: handle exception
				LOGGER.info(
						"Exception occured while deserealizing generative response json to GenerativeModelResponseModel:"
								+ e.getMessage());
				e.printStackTrace();
			} catch (JsonProcessingException e) {
				// TODO: handle exception
				LOGGER.info("Exception occured while deserealizing generative json to GenerativeModelResponseModel:"
						+ e.getMessage());
				e.printStackTrace();
			}

		}

		return mappedVectorsList;

	}

	public static String extractPureJson(String responseFromGenerativeModel) {
		String trimmed = responseFromGenerativeModel.trim();
		String cleanJson = trimmed;
		final String JSON_PREFIX = "```json";
		if (cleanJson.startsWith(JSON_PREFIX)) {
			cleanJson = cleanJson.substring(JSON_PREFIX.length()).trim();
		}
		final String JSON_SUFFIX = "```";
		if (cleanJson.endsWith(JSON_SUFFIX)) {
			int suffixIndex = cleanJson.lastIndexOf(JSON_SUFFIX);
			cleanJson = cleanJson.substring(0, suffixIndex);
		}

		return cleanJson;

	}

	private CandidateDocuments getCandidateDocuments(List<VectorSearchResultsModel> vectorSearchResultsModels) {
		CandidateDocuments candidateDocuments = new CandidateDocuments();
		List<DocumentEntry> documentEntries = new ArrayList<DocumentEntry>();
		candidateDocuments.setDocuments(documentEntries);
		for (VectorSearchResultsModel model : vectorSearchResultsModels) {
			if (null != model) {
				List<String> chunkList = model.getChunkTextList();
				if (null != chunkList && !chunkList.isEmpty()) {
					DocumentEntry documentEntry = new DocumentEntry();
					documentEntry.setId(model.getDocumentId());
					int chucksCounts = Math.min(2, chunkList.size());
					StringBuilder text = new StringBuilder();
					for (int i = 0; i < chucksCounts; i++) {
						if (i != 0) {
							text.append("\n---\n");
						}
						text.append(chunkList.get(i));
					}
					documentEntry.setText(text.toString());
					documentEntries.add(documentEntry);
				}

			}

		}
		return candidateDocuments;
	}

	@Override
	public List<UserDocument> getListofDocumentsForUser(String userid) {
		// TODO Auto-generated method stub
		return StorageUtility.getDocumentsOnUserid(userid);
	}

}
