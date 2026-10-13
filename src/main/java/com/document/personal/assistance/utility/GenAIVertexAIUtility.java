package com.document.personal.assistance.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Logger;
import com.document.personal.assistance.constants.ApplicationConstants;
import com.document.personal.assistance.exception.PrivateDocumentException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;

public class GenAIVertexAIUtility {

	private static final Logger LOGGER = Logger.getLogger(GenAIVertexAIUtility.class.getName());
	private static Client client;
	static {
		try (InputStream inputStream = GenAIVertexAIUtility.class.getClassLoader()
				.getResourceAsStream("AI-ServiceAccount.json")) {
			if (null == inputStream) {
				LOGGER.warning(
						"Inputstream is null. Please ensure AI-ServiceAccount.json is present in src/main/resources");
			} else {
				GoogleCredentials googleCredentials = GoogleCredentials.fromStream(inputStream)
						.createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
				if (null == googleCredentials) {
					LOGGER.warning("GoogleCredentials object is null. Please check application configurations");
				} else {
					client = Client.builder().credentials(googleCredentials).location(ApplicationConstants.Global)
							.project(ApplicationConstants.PROJECT_ID).vertexAI(true).build();
					LOGGER.info("Google GenAI Client sucessfully initialized for Vertex AI.");
				}
			}
		} catch (IOException e) {
			// TODO: handle exception
			LOGGER.info("Exception occured while creating Google Credentials from Service account inputStream: "
					+ e.getMessage());
			e.printStackTrace();
		}
	}

	public static String reRankSummarizeDocuments(String prompttorerankSummarizedocuments) {
		if (null == client) {
			LOGGER.severe("GenAI Client object is null. Please check application initialization logs");
			throw new PrivateDocumentException("Client object is null. Please check application logs", 500);
		}
		try {
			GenerateContentConfig generateContentConfig = GenerateContentConfig.builder().temperature(0.2f).build();
			GenerateContentResponse generateContentResponse = client.models.generateContent(
					ApplicationConstants.liteModelname, prompttorerankSummarizedocuments, generateContentConfig);
			if (null != generateContentResponse && generateContentResponse.text() != null) {
				return generateContentResponse.text();
			} else {
				LOGGER.warning("Received empty response from the model.");
				return "";
			}
		} catch (Exception e) {
			// TODO: handle exception
			String message = e.getMessage();
			LOGGER.severe("Error communicating with vertex AI Gemini API:" + message);
			if (null != message) {
				if (message.contains("has not been used in project") || message.contains("it is disabled")) {
					LOGGER.info(
							"❌ The Vertex AI API is **not enabled** for this project. Please enable it in the Google Cloud Console.");
				} else {
					if (message.contains("PERMISSION_DENIED") || message.contains("aiplatform.endpoints.predict")) {
						LOGGER.info(
								"❌ Access Denied: The service account is missing the Vertex AI User (`roles/aiplatform.user`) role.");
					}
				}
			}
			throw new PrivateDocumentException("Error during document re-ranking/summarization:" + message, 500);
		}
	}


}
