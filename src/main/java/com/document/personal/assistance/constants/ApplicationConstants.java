package com.document.personal.assistance.constants;
/**
@author ANIL LALAM
**/
import java.util.Arrays;
import java.util.List;

public interface ApplicationConstants {

	public static final String DOCUMENT_AI_PROCESSORS_LOCATION = "us";

	public static final String PROJECT_ID = "videoanalyzer-455321";

	public static final String PROCESSOR_ID = "501dfab435adaaeb";

	public static final String LOCATION_ID = "us-central1";
	public static final String modelname = "gemini-2.5-flash";
	// public static final String modelname = "gemini-2.5-pro";

	// Importing: For embeddings, you don't deploy your own endpoint. you call
	// published model directly
	// public static final String TEXT_EMBEDDING_MODEL = "text-embedding-004";
	public static final String TEXT_EMBEDDING_MODEL = "text-multilingual-embedding-002";

	public static final String TASK_TYPE_RETRIEVAL_DOCUMENT = "RETRIEVAL_DOCUMENT";

	public static final String TASK_TYPE_RETRIEVAL_QUERY = "RETRIEVAL_QUERY";

	public static final String DATA_SET = "text_embeddings_dataset";

	public static final String TABLE_NAME = "chunks";

	public static final String BUCKET = "documentassistance";

	public static final String AUDIO_BUCKET = "documentassistanceaudio";

	public static final int MAX_CHUNK_SIZE = 1024;

	public static final int OVERLAPPING = 200;

	public static final String MIME_TYPE_PDF = "application/pdf";

	public static final String FIRESTORE_COLLECTION = "event_log";

	public static final String FIRESTORE_SUMMARY_COLLECTION = "document_summary";

	public static final String FIRESTORE_DATABASE_ID = "privatedocumentsearch";

	public static final int TOP_CHUNK_COUNT = 15;

	public static final float COSINE_DISTANCE_THRESHOLD = 0.35f;

	// Google Authorization URL
	public static final String AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth";

	public static final String INITIATE_RESUMABLE_UPLOAD_URL = "https://storage.googleapis.com/upload/storage/v1/b/"
			+ BUCKET + "/o?uploadType=resumable";

	public static final List<String> SUPPORTED_CONTENT_TYPE = Arrays.asList("image/jpeg", "image/jpg", "image/png",
			"image/bmp", "image/gif", "image/tiff", "application/pdf");

	public static final int NUMBER_OF_CHUNKS_PER_REQUEST = 50;

	public static final float RELEVANCE_GENERATIVE_MODEL_CUTOFF = 0.85f;

	public static final String TEXT_TO_SPEECH_LANGUAGE_CODE = "en-US";

	public static final String AUDIO_CONTENT_TYPE = "audio/mpeg";

}
