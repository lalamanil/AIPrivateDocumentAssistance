package com.document.personal.assistance.utility;
/**
@author ANIL LALAM
**/
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.document.personal.assistance.constants.ApplicationConstants;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessResponse;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.protobuf.ByteString;

public class DocumentOCROnlineInferenceUtility {

	private static final Logger LOGGER = Logger.getLogger(DocumentOCROnlineInferenceUtility.class.getName());

	private static DocumentProcessorServiceClient documentProcessorServiceClient;

	static {
		InputStream inputStream = DocumentOCROnlineInferenceUtility.class.getClassLoader()
				.getResourceAsStream("AI-ServiceAccount.json");
		if (null != inputStream) {
			try {
				GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream)
						.createScoped("https://www.googleapis.com/auth/cloud-platform");
				String endpoint = String.format("%s-documentai.googleapis.com:443",
						ApplicationConstants.DOCUMENT_AI_PROCESSORS_LOCATION);
				DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder()
						.setCredentialsProvider(() -> credentials).setEndpoint(endpoint).build();
				documentProcessorServiceClient = DocumentProcessorServiceClient.create(settings);

			} catch (IOException e) {
				// TODO: handle exception
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}

		} else {

			LOGGER.info(
					"Inputstream for service account AI-ServiceAccount.json is null or empty. Please check application logs");

		}

	}

	public static String escapeNewLines(String text) {

		if (null != text && !text.isEmpty()) {
			return text.replace("\n", "\\n").replace("\r", "\\r");

		}
		return text;

	}

	public static String processDocument(byte[] filedata, String mimeType) {
		String rawText = null;
		if (null != documentProcessorServiceClient) {
			if (null != filedata) {
				String name = String.format("projects/%s/locations/%s/processors/%s", ApplicationConstants.PROJECT_ID,
						ApplicationConstants.DOCUMENT_AI_PROCESSORS_LOCATION, ApplicationConstants.PROCESSOR_ID);
				// converting the image data to a Buffer and base64 encode it.
				ByteString content = ByteString.copyFrom(filedata);
				RawDocument document = RawDocument.newBuilder().setContent(content).setMimeType(mimeType).build();
				// Configure the process request
				ProcessRequest request = ProcessRequest.newBuilder().setName(name).setRawDocument(document).build();
				ProcessResponse processResponse = documentProcessorServiceClient.processDocument(request);
				Document documentResponse = processResponse.getDocument();
				LOGGER.info("Document processing is completed..");
				rawText = documentResponse.getText();
			} else {

				LOGGER.info("filedata bytes is null. Please provide valid byte array.");
			}
		} else {
			LOGGER.info("documentProcessorServiceClient is null. Please check application logs");
		}

		return rawText;

	}

}
