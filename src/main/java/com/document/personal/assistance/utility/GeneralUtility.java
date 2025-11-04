package com.document.personal.assistance.utility;
/**
@author ANIL LALAM
**/
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;
import com.document.personal.assistance.constants.ApplicationConstants;
import com.document.personal.assistance.model.VectorSearchResultsModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.servlet.http.HttpServletRequest;

public class GeneralUtility {

	private static final Logger LOGGER = Logger.getLogger(GeneralUtility.class.getName());

	public static void printHeaders(HttpServletRequest request) {
		Enumeration<String> headernames = request.getHeaderNames();
		LOGGER.info("Headers are.....");
		while (headernames.hasMoreElements()) {
			String headerName = headernames.nextElement();
			System.out.println(headerName + ":" + request.getHeader(headerName));
		}
	}

	public static void printJsonObjectForObject(Object obj) {
		String jsonString = null;
		try {
			jsonString = ObjectMapperSingleton.INSTANCE.getObjectMapper().writeValueAsString(obj);
		} catch (JsonMappingException e) {
			// TODO: handle exception
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		LOGGER.info(jsonString);

	}

	public static List<VectorSearchResultsModel> filterVectorSearchBasedOnRelevenceCutoff(
			List<VectorSearchResultsModel> source, float relevanceCutOff) {
		List<VectorSearchResultsModel> filteredList = new ArrayList<VectorSearchResultsModel>();
		if (null != source && !source.isEmpty()) {
			source.forEach(vsr -> {
				List<Double> distanceList = vsr.getDistanceList();
				if (null != distanceList && !distanceList.isEmpty()) {
					if (distanceList.get(0) < relevanceCutOff) {
						// adding the signed url
						String signedUrl = StorageUtility.generateSignedUrl(ApplicationConstants.BUCKET,
								vsr.getDocumentId());
						vsr.setSignedUrl(signedUrl);
						String documentId = vsr.getDocumentId();
						if (NotNullEmptyUtility.notNullEmptyCheck(documentId)) {
							String[] useridObjectName = documentId.split("/");
							if (null != useridObjectName && useridObjectName.length > 1) {
								vsr.setSummaryText(FireStoreUtility.getSummaryForDocument(useridObjectName[0],
										useridObjectName[1]));
							}

						}
						vsr.setAudiosignedUrl(
								StorageUtility.generateSignedUrl(ApplicationConstants.AUDIO_BUCKET, documentId));
						filteredList.add(vsr);
					}
				}
			});
		}

		return filteredList;
	}

	public static List<VectorSearchResultsModel> filterVectorsFromGenerativeModelOnRelevenceCutoff(
			List<VectorSearchResultsModel> source, float relevenceCutOff) {
		List<VectorSearchResultsModel> filteredList = new ArrayList<VectorSearchResultsModel>();
		if (null != source && !source.isEmpty()) {
			source.forEach(vsr -> {
				if (vsr.getRelevanceScore() >= relevenceCutOff) {
					String signedUrl = StorageUtility.generateSignedUrl(ApplicationConstants.BUCKET,
							vsr.getDocumentId());
					vsr.setSignedUrl(signedUrl);
					String documentId = vsr.getDocumentId();
					if (NotNullEmptyUtility.notNullEmptyCheck(documentId)) {
						String[] userIdObjectName = documentId.split("/");
						if (null != userIdObjectName && userIdObjectName.length > 1) {
							vsr.setSummaryText(
									FireStoreUtility.getSummaryForDocument(userIdObjectName[0], userIdObjectName[1]));
						}
					}
					vsr.setAudiosignedUrl(
							StorageUtility.generateSignedUrl(ApplicationConstants.AUDIO_BUCKET, documentId));
					filteredList.add(vsr);
				}
			});
		}
		return filteredList;
	}

	public static String convertBoldToHtmlOrPlainText(String text, boolean isHtml) {
		String result = text;
		boolean isOpening = true;
		String regex = "\\*\\*";
		while (result.contains("**")) {
			if (isOpening) {
				result = result.replaceFirst(regex, isHtml ? "<b>" : "");
			} else {
				result = result.replaceFirst(regex, isHtml ? "</b>" : "");
			}
			// Toggle the flag for next replacement
			isOpening = !isOpening;
		}
		return result;
	}

}
