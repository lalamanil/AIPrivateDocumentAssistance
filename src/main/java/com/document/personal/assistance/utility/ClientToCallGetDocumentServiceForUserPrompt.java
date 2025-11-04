package com.document.personal.assistance.utility;
/**
@author ANIL LALAM
**/
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import com.document.personal.assistance.model.VectorSearchRequestModel;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientToCallGetDocumentServiceForUserPrompt {

	public static void main_(String[] args) {
		HttpClient httpClient = HttpClient.newBuilder().build();
		VectorSearchRequestModel requestModel = new VectorSearchRequestModel();
		requestModel.setUserId("lalamanilbabu@gmail.com");
		requestModel.setUserPromptForDocSearch("Accident detection model using Vertex AI document please");
		try {
			ObjectMapper mapper = new ObjectMapper();
			String requestedBody = mapper.writeValueAsString(requestModel);
			System.out.println("requestBody:" + requestedBody);
			HttpRequest httpRequest = HttpRequest.newBuilder()
					.uri(URI.create("http://localhost:8080/getDocsforuserprompt"))
					.header("content-Type", "application/json").header("accept", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(requestedBody)).build();
			HttpResponse<String> response = httpClient.send(httpRequest, BodyHandlers.ofString());
			System.out.println("response status Code:" + response.statusCode());
			System.out.println("response body:" + response.body());
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO: handle exception
			e.printStackTrace();
		}

	}

}
