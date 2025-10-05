package com.document.personal.assistance.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import com.document.personal.assistance.constants.ApplicationConstants;
import com.document.personal.assistance.exception.PrivateDocumentException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.aiplatform.v1.PredictRequest;
import com.google.cloud.aiplatform.v1.PredictResponse;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

public class TextEmbeddingWithPredictionServiceUtility {

	private static PredictionServiceClient predictionServiceClient;

	static {
		InputStream inputStream = TextEmbeddingWithPredictionServiceUtility.class.getClassLoader()
				.getResourceAsStream("AI-ServiceAccount.json");
		if (null != inputStream) {
			try {
				GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream)
						.createScoped("https://www.googleapis.com/auth/cloud-platform");
				PredictionServiceSettings settings = PredictionServiceSettings.newBuilder()
						.setCredentialsProvider(() -> credentials).build();
				predictionServiceClient = PredictionServiceClient.create(settings);

			} catch (IOException e) {
				// TODO: handle exception
				e.printStackTrace();
			}

		} else {

			System.out.println("Inputstream for service account is null. Please check application logs");
		}

	}

	public static void predictionRequestCall(List<String> subsetChunkList, PredictRequest.Builder request,
			List<float[]> embeddings, int track, String tasktype) {

		for (String chunk : subsetChunkList) {
			Struct instance = Struct.newBuilder().putFields("content", Value.newBuilder().setStringValue(chunk).build())
					.putFields("task_type", Value.newBuilder().setStringValue(tasktype).build()).build();
			request.addInstances(Value.newBuilder().setStructValue(instance).build());
		}

		PredictRequest predictRequest = request.build();

		PredictResponse predictResponse = predictionServiceClient.predict(predictRequest);

		List<Value> predictionList = predictResponse.getPredictionsList();

		if (null != predictionList && !predictionList.isEmpty()) {

			for (Value prediction : predictionList) {
				List<Value> values = prediction.getStructValue().getFieldsOrThrow("embeddings").getStructValue()
						.getFieldsOrThrow("values").getListValue().getValuesList();
				float[] vector = new float[values.size()];

				for (int j = 0; j < values.size(); j++) {
					vector[j] = (float) values.get(j).getNumberValue();
				}
				embeddings.add(vector);
			}

		} else {

			System.out.println("predictionList for track:" + track + " is null or empty");
		}

	}

	public static List<float[]> getEmbeddings(List<String> chunkList, String taskType) {
		List<float[]> embeddings = new ArrayList<float[]>();
		if (null != chunkList && !chunkList.isEmpty()) {
			if (null != predictionServiceClient) {
				// Important: For embeddings, you don't deploy your own endpoint.
				// You call the published model directly.
				String endPointName = String.format("projects/%s/locations/%s/publishers/google/models/%s",
						ApplicationConstants.PROJECT_ID, ApplicationConstants.LOCATION_ID,
						ApplicationConstants.TEXT_EMBEDDING_MODEL);
				System.out.println("endPointName is:" + endPointName);

				int exactChuckSize = chunkList.size() / 100;
				int leftOverChunk = chunkList.size() % 100;
				int track = 0;
				if (exactChuckSize > 0) {
					for (int i = 0; i < exactChuckSize; i++) {

						// predictRequest
						PredictRequest.Builder request = PredictRequest.newBuilder().setEndpoint(endPointName);
						List<String> subsetChunkList = chunkList.subList(track, track + 100);
						predictionRequestCall(subsetChunkList, request, embeddings, track, taskType);
						track = track + 100;
						System.out.println("***** at track:" + track);
					}
				}

				if (leftOverChunk > 0) {
					// predictRequest
					PredictRequest.Builder request = PredictRequest.newBuilder().setEndpoint(endPointName);
					List<String> subsetChunkList = chunkList.subList(track, chunkList.size());
					predictionRequestCall(subsetChunkList, request, embeddings, track, taskType);
					System.out.println("**** at leftOver track:" + (chunkList.size() - track));
				}

			} else {
				System.out.println("predictionServiceClient is null. Please check your application logs");
				throw new PrivateDocumentException(
						"predictionServiceClient is null. Please check your application logs", 500);
			}
		} else {
			System.out.println("chunlklist is null or empty.");
			throw new PrivateDocumentException("chunlklist is null or empty for userprompt.", 500);
		}

		return embeddings;
	}

}
