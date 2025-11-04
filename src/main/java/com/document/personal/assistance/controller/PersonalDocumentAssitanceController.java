package com.document.personal.assistance.controller;
/**
@author ANIL LALAM
**/
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.document.personal.assistance.constants.EventStoreStatus;
import com.document.personal.assistance.model.EventArcPayLoadModel;
import com.document.personal.assistance.model.EventArcResponseModel;
import com.document.personal.assistance.model.IdTokenModel;
import com.document.personal.assistance.model.PrivateDocumentErrorModel;
import com.document.personal.assistance.model.UserDocument;
import com.document.personal.assistance.model.UserPromptResponseModel;
import com.document.personal.assistance.model.VectorSearchRequestModel;
import com.document.personal.assistance.model.VectorSearchResultsModel;
import com.document.personal.assistance.service.EmbeddingService;
import com.document.personal.assistance.service.GcsResumableUpload;
import com.document.personal.assistance.service.RedirectGoogleOauth2ServerService;
import com.document.personal.assistance.utility.FireStoreUtility;
import com.document.personal.assistance.utility.GeneralUtility;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Controller
@RequestMapping(value = "/")
public class PersonalDocumentAssitanceController {

	private static final Logger LOGGER = Logger.getLogger(PersonalDocumentAssitanceController.class.getName());

	@Autowired(required = true)
	private EmbeddingService embeddingService;

	@Autowired(required = true)
	private RedirectGoogleOauth2ServerService redirectGoogleOauth2ServerService;

	@Autowired(required = true)
	private GcsResumableUpload gcsResumableUpload;

	@Hidden
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public void redirectToGoogle(HttpServletResponse response) throws IOException {

		redirectGoogleOauth2ServerService.redirectToAuthorizationURL(response);
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

	@RequestMapping(value = "/userdocsbyprompt", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Operation(summary = "Fetch contextually relevant documents for a user query", description = "This endpoint accepts a user ID and a natural language prompt. The prompt is converted into embeddings (vectors) using the text-multimodal-embedding model with the 'Retrieval_Query' task type. A semantic search is then performed on the user's stored documents to return the most contextually relevant results.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Request succeeded. The response may contain relevant documents or an empty list if none were found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserPromptResponseModel.class), examples = {
					@ExampleObject(value = "{\"documents\":[{\"documentId\":\"lalamanilbabu@gmail.com/594811_CMM.pdf\",\"distanceList\":[0.2856592128922749,0.2858003739131092],\"numberOfChunks\":7,\"signedUrl\":“****”,\"contentType\":\"application/pdf\",\"relevanceScore\": 1,\"shortReason\":“short description“,\"audiosignedUrl\": “*****”,\"createdAt\":\"2025-10-20T21:09:57.629\" } ],\"message\":\"Relevant Documents Found\",\"statusCode\": 200}", name = "Relevant Documents Found"),
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

	@RequestMapping(value = "/listuserdocs", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Operation(summary = "Retrieve documents uploaded by a user", description = "This endpoint returns the list of documents uploaded by the specified user to the GCS bucket.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved the list of documents", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDocument.class), examples = @ExampleObject(value = "[{\"name\":\"594811_Btech_OD.pdf\",\"fullName\":\"lalamanilbabu@gmail.com/594811_Btech_OD.pdf\",\"mimeType\": \"application/pdf\",\"signedUrl\":\"https://storage.googleapis.com/documentassistance/lalamanilbabu%40gmail.com/594811_Btech_OD.pdf?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=ai-projects-service-account%40videoanalyzer-455321.iam.gserviceaccount.com%2F20251031%2Fauto%2Fstorage%2Fgoog4_request&X-Goog-Date=20251031T032352Z&X-Goog-Expires=3600&X-Goog-SignedHeaders=host&X-Goog-Signature=...\",\"summaryText\": “Summary text,\"audiosignedUrl\": “***”}]"))),
			@ApiResponse(responseCode = "400", description = "Bad Request - Missing or invalid user ID", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PrivateDocumentErrorModel.class), examples = @ExampleObject(value = "{\"statusCode\": 400, \"message\": \"userid is required. Can not be null or empty\", \"path\": \"/listUserDocs\", \"timeStamp\": \"2025-10-02T19:03:59.081638Z\"}"))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error - Unexpected issue occurred", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PrivateDocumentErrorModel.class), examples = @ExampleObject(value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Unexpected processing error\", \"path\": \"/event\", \"timestamp\": \"2025-10-01T12:30:45Z\" }")))

	})
	public ResponseEntity<List<UserDocument>> listUserDocuments(
			@Parameter(description = "User ID (email) whose documents need to be retrieved", example = "lalamanilbabu@gmail.com") @NotBlank @RequestParam(required = true, name = "userid") String userid) {
		LOGGER.info("Request param userid:" + userid);
		return new ResponseEntity<List<UserDocument>>(embeddingService.getListofDocumentsForUser(userid.trim()),
				HttpStatus.OK);
	}

	@Hidden
	@RequestMapping(value = "/oauth", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public String oauthAuthorizationCode(@RequestParam Map<String, String> queryParams,
			HttpServletResponse servletResponse, Model model) {

		IdTokenModel user = redirectGoogleOauth2ServerService.getAccessTokenFromCode(queryParams);

		if (null != user) {
			model.addAttribute("name", user.getName());
			model.addAttribute("email", user.getEmail());
			model.addAttribute("picture", user.getPicture());
		}

		return "welcome.html";
	}

	@RequestMapping(value = "/initiateResumableUpload", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Operation(summary = "Initiate a Resumable Upload Session", description = "Starts a resumable upload session for large files to be uploaded to a GCS bucket in chunks. "
			+ "This service returns a session URI that can be used to upload data in multiple parts.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Request successful. The response includes a session URL for uploading file chunks.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class), examples = @ExampleObject(value = "{\n"
					+ "  \"uploadUrl\": \"https://storage.googleapis.com/upload/storage/v1/b/documentassistance/o?uploadType=resumable&upload_id=**********\"\n"
					+ "}"))),
			@ApiResponse(responseCode = "400", description = "Bad Request – Missing or invalid 'objectName' or 'contentType' parameters.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PrivateDocumentErrorModel.class), examples = @ExampleObject(value = "{\n"
					+ "  \"statusCode\": 400,\n"
					+ "  \"message\": \"Required query parameters [objectName, contentType] cannot be null or empty\",\n"
					+ "  \"path\": \"/initiateResumableUpload\",\n"
					+ "  \"timeStamp\": \"2025-10-09T20:25:20.265504Z\"\n" + "}"))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error – An unexpected issue occurred during processing.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PrivateDocumentErrorModel.class), examples = @ExampleObject(value = "{\n"
					+ "  \"status\": 500,\n" + "  \"error\": \"Internal Server Error\",\n"
					+ "  \"message\": \"Unexpected processing error\",\n"
					+ "  \"path\": \"/initiateResumableUpload\",\n" + "  \"timestamp\": \"2025-10-01T12:30:45Z\"\n"
					+ "}"))) })
	public ResponseEntity<Map<String, String>> initiateResumableUpload(
			@Parameter(description = "Name of the object", example = "lalamanilbabu@gmail.com/ssc.pdf") @NotBlank @RequestParam(name = "objectName", required = true) String objectName,
			@Parameter(description = "ContentType of the object", example = "application/pdf") @NotBlank @RequestParam(name = "contentType", required = true) String contentType) {

		return ResponseEntity.ok(gcsResumableUpload.initiateResumableUploadSessionUri(objectName, contentType));

	}

	@Hidden
	@RequestMapping(value = "/uploadChunks", method = RequestMethod.POST, consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public ResponseEntity<?> uploadChunk(
			@NotBlank @RequestHeader(name = "SessionUrl", required = true) String sessionUrl,
			@NotBlank @RequestHeader(name = "Content-Range", required = true) String contentRange,
			@NotBlank @RequestHeader(name = "Content-Length", required = true) String contentLength,
			@RequestBody(required = true) byte[] chunk) {
		Map<String, String> responseMap = gcsResumableUpload.uploadChunk(sessionUrl, contentRange, contentLength,
				chunk);
		String statusCodestr = responseMap.getOrDefault("statusCode", "500");
		String responseBody = responseMap.getOrDefault("responseBody", "Unknown error");
		int statusCode;
		try {
			statusCode = Integer.valueOf(statusCodestr);
		} catch (NumberFormatException e) {
			// TODO: handle exception
			statusCode = 500;
		}
		return ResponseEntity.status(statusCode).body(responseBody);
	}

	@RequestMapping(value = "/uploadPage", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String uploadFile() {

		return "upload :: content";
	}

	@RequestMapping(value = "/documentsPage", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String documentsPage() {

		return "documents :: content";
	}

	@RequestMapping(value = "/chatPage", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String chatPage() {

		return "chat :: content";
	}

	@RequestMapping(value = "/technicaldocPage", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String getTechnicalDocPage() {

		return "technicaldoc :: content";
	}

	@RequestMapping(value = "/contactPage", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String getContactPage() {
		return "contact :: content";
	}

	@Hidden
	@RequestMapping("/favicon.ico")
	@ResponseBody
	public ResponseEntity<Void> favicon() {
		return ResponseEntity.ok().build();
	}

}
