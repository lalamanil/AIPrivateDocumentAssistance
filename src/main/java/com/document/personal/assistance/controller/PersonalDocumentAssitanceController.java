package com.document.personal.assistance.controller;

import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.document.personal.assistance.constants.EventStoreStatus;
import com.document.personal.assistance.model.EventArcPayLoadModel;
import com.document.personal.assistance.model.EventArcResponseModel;
import com.document.personal.assistance.model.PrivateDocumentErrorModel;
import com.document.personal.assistance.model.UserPromptResponseModel;
import com.document.personal.assistance.model.VectorSearchRequestModel;
import com.document.personal.assistance.model.VectorSearchResultsModel;
import com.document.personal.assistance.service.EmbeddingService;
import com.document.personal.assistance.utility.FireStoreUtility;
import com.document.personal.assistance.utility.GeneralUtility;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Controller
@RequestMapping(value = "/")
public class PersonalDocumentAssitanceController {

	private static final Logger LOGGER = Logger.getLogger(PersonalDocumentAssitanceController.class.getName());

	@Autowired
	private EmbeddingService embeddingService;

	@RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	@ResponseBody
	public ResponseEntity<String> healthCheck() {
		return ResponseEntity.ok("Application is healthy");
	}

	@RequestMapping(value = "/event", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Operation(summary = "Handle GCS Event", description = "Processes an incoming GCS event and orchestrates the workflow: "
			+ "retrieves the document from GCS, extracts raw text using Document OCR, "
			+ "splits the text into chunks, generates embeddings using the text-multilingual-embedding model, "
			+ "and persists the resulting vectors into Google BigQuery.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Event processed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventArcResponseModel.class), examples = @ExampleObject(value = "{ \"status\": \"Event Processed.\", \"timestamp\": \"2025-10-02T00:23:48.186776Z\"}"))),
			@ApiResponse(responseCode = "400", description = "Bad Request - Missing headers,Invalid inputs or validation failed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PrivateDocumentErrorModel.class), examples = @ExampleObject(value = "{\"statusCode\": 400,\"message\": \"Either the bucket Name or Object Name is invalid.\",\"path\": \"/event\", \"timeStamp\": \"2025-10-02T00:20:56.808176Z\"}"))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error - Unexpected issue occurred", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PrivateDocumentErrorModel.class), examples = @ExampleObject(value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Unexpected processing error\", \"path\": \"/event\", \"timestamp\": \"2025-10-01T12:30:45Z\" }")))

	})
	public ResponseEntity<EventArcResponseModel> handleGCSEvent(
			@RequestHeader(name = "ce-id", required = true) String ceId,
			@RequestHeader(name = "ce-bucket", required = true) String ceBucket,
			@Valid @RequestBody(required = true) EventArcPayLoadModel eventarcPayload, HttpServletRequest request) {
		GeneralUtility.printHeaders(request);
		EventStoreStatus eventStoreStatus = FireStoreUtility.saveUniqueEvent(ceId, ceBucket, eventarcPayload.getName(),
				eventarcPayload.getGeneration(), eventarcPayload.getContentType());
		if (EventStoreStatus.SUCCESS.name().equalsIgnoreCase(eventStoreStatus.name())) {
			GeneralUtility.printJsonObjectForObject(eventarcPayload);
			String objectName = eventarcPayload.getName();
			String bucket = eventarcPayload.getBucket();
			LOGGER.info("objectName:" + objectName);
			LOGGER.info("bucket:" + bucket);
			embeddingService.embedthefiledropedtoGCSBucket(bucket, objectName);
		}
		return ResponseEntity.ok(new EventArcResponseModel("Event Processed.", Instant.now()));
	}

	@RequestMapping(value = "/getDocsforuserprompt", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Operation(summary = "Fetch contextually relevant documents for a user query", description = "This endpoint accepts a user ID and a natural language prompt. The prompt is converted into embeddings (vectors) using the text-multimodal-embedding model with the 'Retrieval_Query' task type. A semantic search is then performed on the user's stored documents to return the most contextually relevant results.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Request succeeded. The response may contain relevant documents or an empty list if none were found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserPromptResponseModel.class), examples = {
					@ExampleObject(value = "{\"documents\":[{\"documentId\":\"lalamanilbabu@gmail.com/Building_Accident_Detection_System_Using_Java_GoogleCloud_VertexAI.pdf\",\"distanceList\":[0.2756992966033063,0.28688146218213717,0.2886615079718411],\"numberOfChunks\":3,\"createdAt\":\"2025-10-02T16:22:00.944\"}],\"message\":\"relevant documents are found\",\"statusCode\":200}", name = "Relevant Documents Found"),
					@ExampleObject(name = "No Relevant Documents Found", value = "{\"documents\":[],\"message\":\"No relevant documents found for the given prompt\",\"statusCode\":200}") })),
			@ApiResponse(responseCode = "400", description = "Bad Request - Missing headers,Invalid inputs or validation failed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PrivateDocumentErrorModel.class), examples = @ExampleObject(value = "{\"statusCode\": 400,\"message\": \"userid cannot be null or empty. Please provide valid userid\",\"path\": \"/getDocsforuserprompt\", \"timeStamp\": \"2025-10-02T01:23:55.801846Z\"}"))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error - Unexpected issue occurred", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PrivateDocumentErrorModel.class), examples = @ExampleObject(value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Unexpected processing error\", \"path\": \"/event\", \"timestamp\": \"2025-10-01T12:30:45Z\" }"))) })
	public ResponseEntity<UserPromptResponseModel> getDocmentsForUserPrompt(
			@Valid @RequestBody(required = true) VectorSearchRequestModel vectorSearchRequestModel) {
		List<VectorSearchResultsModel> vectorSearchResultsModels = embeddingService.searchDocumentsForUserPrompt(
				vectorSearchRequestModel.getUserId(), vectorSearchRequestModel.getUserPromptForDocSearch(),
				vectorSearchRequestModel.getRelevanceCutoff());
		UserPromptResponseModel userPromptResponseModel = new UserPromptResponseModel();
		userPromptResponseModel.setDocuments(vectorSearchResultsModels);
		if (vectorSearchResultsModels.isEmpty()) {
			userPromptResponseModel.setMessage("No Relevant Documents Found for the given prompt");
		} else {
			userPromptResponseModel.setMessage("Relevant Documents Found");
		}
		userPromptResponseModel.setStatusCode(200);
		return new ResponseEntity<UserPromptResponseModel>(userPromptResponseModel, HttpStatus.OK);

	}

	@RequestMapping(value = "/listUserDocs", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Operation(summary = "Retrieve documents uploaded by a user", description = "This endpoint returns the list of documents uploaded by the specified user to the GCS bucket.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved the list of documents", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class), examples = @ExampleObject(value = "[\"lalamanilbabu@gmail.com/594811_825090_i129.pdf\",\"lalamanilbabu@gmail.com/594811_Btech_OD.pdf\",\"lalamanilbabu@gmail.com/594811_CMM.pdf\",\"lalamanilbabu@gmail.com/594811_Resume.pdf\",\"lalamanilbabu@gmail.com/AIPoweredVideoSummarizationandMultilingualNarration.pdf\",\"lalamanilbabu@gmail.com/Building_Accident_Detection_System_Using_Java_GoogleCloud_VertexAI.pdf\",\"lalamanilbabu@gmail.com/DeployingaSpringBootApplicationoGoogleCloudRun.pdf\",\"lalamanilbabu@gmail.com/EVALUATIONOFACADEMICCREDENTIALS.pdf\",\"lalamanilbabu@gmail.com/I-200_]"))),
			@ApiResponse(responseCode = "400", description = "Bad Request - Missing or invalid user ID", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PrivateDocumentErrorModel.class), examples = @ExampleObject(value = "{\"statusCode\": 400, \"message\": \"userid is required. Can not be null or empty\", \"path\": \"/listUserDocs\", \"timeStamp\": \"2025-10-02T19:03:59.081638Z\"}"))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error - Unexpected issue occurred", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PrivateDocumentErrorModel.class), examples = @ExampleObject(value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Unexpected processing error\", \"path\": \"/event\", \"timestamp\": \"2025-10-01T12:30:45Z\" }")))

	})
	public ResponseEntity<List<String>> listUserDocuments(
			@Parameter(description = "User ID (email) whose documents need to be retrieved", example = "lalamanilbabu@gmail.com") @NotBlank @RequestParam(required = true, name = "userid") String userid) {
		LOGGER.info("Request param userid:" + userid);
		return new ResponseEntity<List<String>>(embeddingService.getListofDocumentsForUser(userid.trim()),
				HttpStatus.OK);
	}

	@RequestMapping(value = "/oauth", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	@ResponseBody
	public ResponseEntity<String> oauthAuthorizationCode(HttpServletRequest request) {

		String queryString = request.getQueryString();

		LOGGER.info("query string:" + queryString);

		return ResponseEntity.ok("Sucessfull");

	}

}
