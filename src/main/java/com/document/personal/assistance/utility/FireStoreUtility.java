package com.document.personal.assistance.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import com.document.personal.assistance.constants.ApplicationConstants;
import com.document.personal.assistance.constants.EventStoreStatus;
import com.document.personal.assistance.exception.PrivateDocumentException;
import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

public class FireStoreUtility {

	private static final Logger LOGGER = Logger.getLogger(FireStoreUtility.class.getName());
	private static Firestore firestore;
	static {
		InputStream inputStream = FireStoreUtility.class.getClassLoader().getResourceAsStream("AI-ServiceAccount.json");
		if (null != inputStream) {
			try {
				GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream);
				firestore = FirestoreOptions.newBuilder().setCredentials(credentials)
						.setDatabaseId(ApplicationConstants.FIRESTORE_DATABASE_ID).build().getService();
			} catch (IOException e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		} else {
			LOGGER.info("service account inputstream is null. Please check application logs.");
		}

	}

	public static EventStoreStatus saveUniqueEvent(String eventId, String bucket, String objectName, String generation,
			String contentType) {
		if (NotNullEmptyUtility.notNullEmptyCheck(eventId) && NotNullEmptyUtility.notNullEmptyCheck(bucket)
				&& NotNullEmptyUtility.notNullEmptyCheck(objectName)
				&& NotNullEmptyUtility.notNullEmptyCheck(contentType)) {
			DocumentReference documentReference = firestore.collection(ApplicationConstants.FIRESTORE_COLLECTION)
					.document(eventId);
			try {
				Map<String, Object> data = new HashMap<String, Object>();
				data.put("bucket", bucket);
				data.put("objectName", objectName);
				data.put("generation", generation);
				data.put("contentType", contentType);
				data.put("processedAt", FieldValue.serverTimestamp());
				documentReference.create(data).get();
				LOGGER.info("Event stored Successfully:" + eventId);
				return EventStoreStatus.SUCCESS;
			} catch (ExecutionException e) {
				// TODO: handle exception
				if (e.getCause() instanceof AlreadyExistsException) {
					LOGGER.info("Duplicate event detected:" + eventId);
					return EventStoreStatus.DUPLICATE;
				} else {
					LOGGER.severe("Error storing event " + eventId + ":" + e.getMessage());
					throw new PrivateDocumentException("Error storing event " + eventId + ":" + e.getMessage(), 500);
				}

			} catch (InterruptedException e) {
				// TODO: handle exception
				Thread.currentThread().interrupt();
				throw new PrivateDocumentException("Interruption Exception:" + e.getMessage(), 500);
			}

		} else {
			LOGGER.info(
					"Header parameter ce-id,ce-bucket and  name,contentType in request body cannot be null or empty.");
			throw new PrivateDocumentException(
					"Header parameter ce-id,ce-bucket and  name,contentType in request body cannot be null or empty.",
					400);
		}

	}

}
