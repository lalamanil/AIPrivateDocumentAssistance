package com.document.personal.assistance.service.impl;
/**
@author ANIL LALAM
**/
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;
import com.document.personal.assistance.constants.ApplicationConstants;
import com.document.personal.assistance.exception.PrivateDocumentException;
import com.document.personal.assistance.service.GcsResumableUpload;
import com.document.personal.assistance.utility.AccessTokenUtility;
import com.document.personal.assistance.utility.NotNullEmptyUtility;
import com.document.personal.assistance.utility.ObjectMapperSingleton;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
public class GcsResumableUploadImpl implements GcsResumableUpload {

	private static final Logger LOGGER = Logger.getLogger(GcsResumableUploadImpl.class.getName());

	@Override
	public Map<String, String> initiateResumableUploadSessionUri(String objectName, String contentType) {
		// TODO Auto-generated method stub

		if (!NotNullEmptyUtility.notNullEmptyCheck(objectName)) {
			LOGGER.info("objectName cannot be null or empty");
			throw new PrivateDocumentException("objectName cannot be null or empty", 400);
		}

		if (!NotNullEmptyUtility.notNullEmptyCheck(contentType)) {
			LOGGER.info("Object contentType cannot be null or empty");
			throw new PrivateDocumentException("object ContentName cannot be null or empty", 400);
		}

		if (!ApplicationConstants.SUPPORTED_CONTENT_TYPE.contains(contentType)) {
			LOGGER.info(contentType + " is not supported. Supported contentTpes are:"
					+ ApplicationConstants.SUPPORTED_CONTENT_TYPE);
			throw new PrivateDocumentException(contentType + " is not supported. Supported contentTpes are:"
					+ ApplicationConstants.SUPPORTED_CONTENT_TYPE, 400);

		}

		String accessToken = AccessTokenUtility.getAccessToken();
		if (null == accessToken) {
			LOGGER.info("accessToken is null or empty.");
			throw new PrivateDocumentException("accessToken is null or empty", 500);
		}
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put("name", objectName);
		metadata.put("contentType", contentType);
		try {
			String metadataJson = ObjectMapperSingleton.INSTANCE.getObjectMapper().writeValueAsString(metadata);
			LOGGER.info("metadataJson:" + metadataJson);
			// Http Client
			HttpClient httpClient = HttpClient.newBuilder().version(Version.HTTP_2).build();

			LOGGER.info(ApplicationConstants.INITIATE_RESUMABLE_UPLOAD_URL);
			// Build HttpRequest
			HttpRequest httpRequest = HttpRequest.newBuilder()
					.uri(URI.create(ApplicationConstants.INITIATE_RESUMABLE_UPLOAD_URL)).timeout(Duration.ofSeconds(20))
					.header("Authorization", "Bearer " + accessToken)
					.header("Content-Type", "application/json; charset=UTF-8")
					.POST(BodyPublishers.ofString(metadataJson)).build();

			// Send Http Request
			HttpResponse<Void> httpResponse = httpClient.send(httpRequest, BodyHandlers.discarding());

			if (200 != httpResponse.statusCode() && 201 != httpResponse.statusCode()) {

				String msg = String.format("Failed to initiate resumable upload. HTTP %d", httpResponse.statusCode());
				LOGGER.info(msg);
				throw new PrivateDocumentException(msg, 500);
			}

			String uploadSessionUrl = httpResponse.headers().firstValue("Location")
					.orElseThrow(() -> new PrivateDocumentException("No Location header returned from GCS", 500));

			LOGGER.info("uploaded Session url:" + uploadSessionUrl);

			return Map.of("uploadUrl", uploadSessionUrl);

		} catch (JsonProcessingException e) {
			// TODO: handle exception
			String msg = "Exception while converting metadata object to json:" + e.getMessage();
			LOGGER.info(msg);
			throw new PrivateDocumentException(msg, 500);
		} catch (InterruptedException e) {
			// TODO: handle exception
			e.printStackTrace();
			Thread.currentThread().interrupt();
			throw new PrivateDocumentException("Interrupted while Initiating Resumable upload to get session uri", 500);

		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
			throw new PrivateDocumentException("I/O exception while Initiating Resumable upload to session uri", 500);
		}

	}

	@Override
	public Map<String, String> uploadChunk(String sessionUrl, String contentRange, String contentLength, byte[] chunk) {
		// TODO Auto-generated method stub

		LOGGER.info("sessionurl:" + sessionUrl);
		LOGGER.info("contentRange:" + contentRange);
		LOGGER.info("contentLength:" + contentLength);

		try {
			HttpClient httpClient = HttpClient.newBuilder().version(Version.HTTP_2).build();

			HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(sessionUrl))
					.header("Content-Range", contentRange).header("Content-Type", "application/octet-stream")
					.PUT(BodyPublishers.ofByteArray(chunk)).build();
			HttpResponse<String> response = httpClient.send(httpRequest, BodyHandlers.ofString());
			int statusCode = response.statusCode();
			LOGGER.info("GCS responded with status:" + statusCode);

			if (200 == statusCode) {

				LOGGER.info("upload Completed. final Metadata:" + response.body());

				return Map.of("statusCode", "200", "responseBody", response.body());

			} else {

				if (308 == statusCode) {

					// partial success- continue uploading next chunk
					LOGGER.info("Chunk uploaded successfully. Continue with next Chunk");
					return Map.of("statusCode", "308", "responseBody", "Chunk uploaded, continue...");

				} else {

					LOGGER.info("Chunk upload failed with status " + statusCode + " : " + response.body());

					return Map.of("statusCode", String.valueOf(statusCode), "responseBody",
							"GCS Error: " + response.body());
				}
			}

		} catch (InterruptedException e) {
			// TODO: handle exception
			e.printStackTrace();
			Thread.currentThread().interrupt();
			throw new PrivateDocumentException("Interrupted while sending the chunks to GCS bucket", 500);
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
			throw new PrivateDocumentException("I/O exception while sending the chunks to GCS bucket", 500);
		}

	}

}
