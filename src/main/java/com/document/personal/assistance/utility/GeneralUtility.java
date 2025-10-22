package com.document.personal.assistance.utility;

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
						filteredList.add(vsr);
					}
				}
			});
		}

		return filteredList;
	}

}
