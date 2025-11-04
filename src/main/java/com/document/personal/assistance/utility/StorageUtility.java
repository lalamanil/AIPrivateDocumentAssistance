package com.document.personal.assistance.utility;
/**
@author ANIL LALAM
**/
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import com.document.personal.assistance.constants.ApplicationConstants;
import com.document.personal.assistance.exception.PrivateDocumentException;
import com.document.personal.assistance.model.UserDocument;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Storage.BlobListOption;

public class StorageUtility {

	private static final Logger LOGGER = Logger.getLogger(StorageUtility.class.getName());

	private static Storage storage;

	static {
		InputStream inputStream = StorageUtility.class.getClassLoader().getResourceAsStream("AI-ServiceAccount.json");
		if (null != inputStream) {
			try {
				GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream)
						.createScoped("https://www.googleapis.com/auth/cloud-platform");
				storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
			} catch (IOException e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		} else {
			System.out.println("Inputstream for service account is null or empty. Please check application logs..");
		}

	}

	public static void writeObjectToBucket(String bucketName, String objectName, byte[] content, String contentType) {
		if (null != storage) {
			if (null != bucketName && null != objectName && null != content && null != contentType) {
				BlobId blobId = BlobId.of(bucketName, objectName);
				BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();
				try {
					Blob savedBlob = storage.create(blobInfo, content);
					System.out.println(savedBlob.getBucket() + ":" + savedBlob.getName());
				} catch (StorageException e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			} else {
				System.out.println("bucketName,objectName,content and contentType are required. Cannot be null.");
			}
		} else {

			System.out.println("storage object is null. Please check application logs.");
			throw new PrivateDocumentException("storage object is null. Please check application logs", 500);
		}
	}

	public static Map<String, Object> getObjectFromBucket(String bucketName, String objectName) {
		Map<String, Object> contentMap = new HashMap<String, Object>();
		if (null != storage) {
			try {
				Blob blob = storage.get(bucketName, objectName);
				if (null != blob) {
					byte[] content = blob.getContent();
					String contentType = blob.getContentType();
					contentMap.put("content", content);
					contentMap.put("contentType", contentType);
				}
			} catch (StorageException e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		} else {
			System.out.println("storage object is null. Please check application logs");
			throw new PrivateDocumentException("storage object is null. Please check application logs", 500);
		}
		return contentMap;
	}

	public static List<UserDocument> getDocumentsOnUserid(String userid) {
		List<UserDocument> documentList = new ArrayList<UserDocument>();
		if (NotNullEmptyUtility.notNullEmptyCheck(userid)) {
			if (null != storage) {
				try {
					Bucket bucket = storage.get(ApplicationConstants.BUCKET);
					if (null == bucket) {
						LOGGER.info(ApplicationConstants.BUCKET + " is not found. Please provide valid bucket Name");
						throw new PrivateDocumentException(
								ApplicationConstants.BUCKET + " is not found. Please provide valid bucket Name", 400);
					}
					Iterable<Blob> iterable = bucket.list(BlobListOption.prefix(userid + "/")).iterateAll();
					for (Blob blob : iterable) {
						if (!blob.isDirectory()) {
							String name = blob.getName().substring(blob.getName().lastIndexOf("/") + 1);
							documentList.add(new UserDocument(name, blob.getName(), blob.getContentType(),
									generateSignedUrl(ApplicationConstants.BUCKET, blob.getName()),
									FireStoreUtility.getSummaryForDocument(userid, name),
									generateSignedUrl(ApplicationConstants.AUDIO_BUCKET, blob.getName())));
						}
					}
					return documentList;
				} catch (StorageException e) {
					// TODO: handle exception
					e.printStackTrace();
					throw new PrivateDocumentException("Storage Exception:" + e.getMessage(), 500);
				}
			} else {
				System.out.println("storage object is null. Please check application logs");
				throw new PrivateDocumentException("storage object is null. Please check application logs", 500);
			}

		} else {
			LOGGER.info("userid is required. Can not be null or empty");
			throw new PrivateDocumentException("userid is required. Can not be null or empty", 400);
		}

	}

	public static String generateSignedUrl(String bucketName, String objectName) {
		if (null == storage) {
			LOGGER.info("storage object is null. Please check application logs");
			throw new PrivateDocumentException("storage object is null. Please check application logs", 500);
		}
		if (!NotNullEmptyUtility.notNullEmptyCheck(bucketName)) {
			LOGGER.info("bucket Name cannot be null or empty");
			throw new PrivateDocumentException("bucketName cannot be null or empty", 400);
		}
		if (!NotNullEmptyUtility.notNullEmptyCheck(objectName)) {
			LOGGER.info("objectName cannot be null or empty");
			throw new PrivateDocumentException("objectName cannot be null or empty", 400);
		}

		BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName).build();
		URL url = storage.signUrl(blobInfo, 1, TimeUnit.HOURS, Storage.SignUrlOption.withV4Signature());
		String signedUrl = url.toString();
		LOGGER.info("signedUrl for bucketName:" + bucketName + " objectName:" + objectName + " is:" + signedUrl);
		return signedUrl;
	}

	public static void main_(String[] args) {

		String fileName = "/Users/lalamanil/voiceanalyzer/documents/DeployingaSpringBootApplicationoGoogleCloudRun.pdf";
		try {
			byte[] buffer = Files.readAllBytes(Paths.get(fileName));
			writeObjectToBucket(ApplicationConstants.BUCKET,
					"lalamanilbabu@gmail.com/DeployingaSpringBootApplicationoGoogleCloudRun.pdf", buffer,
					"application/pdf");
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}
