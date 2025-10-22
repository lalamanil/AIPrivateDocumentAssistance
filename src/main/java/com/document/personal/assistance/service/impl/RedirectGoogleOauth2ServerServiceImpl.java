package com.document.personal.assistance.service.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.document.personal.assistance.constants.ApplicationConstants;
import com.document.personal.assistance.exception.TokenException;
import com.document.personal.assistance.model.IdTokenModel;
import com.document.personal.assistance.model.TokenModel;
import com.document.personal.assistance.service.RedirectGoogleOauth2ServerService;
import com.document.personal.assistance.utility.NotNullEmptyUtility;
import com.document.personal.assistance.utility.ObjectMapperSingleton;

import jakarta.servlet.http.HttpServletResponse;

@Service
public class RedirectGoogleOauth2ServerServiceImpl implements RedirectGoogleOauth2ServerService {

	private static final Logger LOGGER = Logger.getLogger(RedirectGoogleOauth2ServerServiceImpl.class.getName());

	@Value("${client_id}")
	private String client_id;

	@Value("${client_secret}")
	private String client_secret;

	@Value("${redirect_uri}")
	private String redirect_uri;

	@Value("${scope}")
	private String scope;

	@Value("${access_type}")
	private String access_type;

	@Value("${response_type}")
	private String response_type;

	@Value("${state}")
	private String state;

	@Value("${prompt}")
	private String prompt;

	@Override
	public void redirectToAuthorizationURL(HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub

		StringBuilder builder = new StringBuilder();

		builder.append(ApplicationConstants.AUTHORIZATION_URL);

		if (!NotNullEmptyUtility.notNullEmptyCheck(redirect_uri)) {
			writeErrorResponse("redirect_uri is null or empty", "400", response);
			return;
		}

		if (!NotNullEmptyUtility.notNullEmptyCheck(client_id)) {
			writeErrorResponse("client_id is null or empty", "400", response);
			return;
		}

		if (!NotNullEmptyUtility.notNullEmptyCheck(scope)) {

			writeErrorResponse("scope is null or empty", "400", response);
			return;
		}

		if (!NotNullEmptyUtility.notNullEmptyCheck(access_type)) {

			writeErrorResponse("access_type is null or empty", "400", response);
			return;
		}

		if (!NotNullEmptyUtility.notNullEmptyCheck(response_type)) {
			writeErrorResponse("response_type is null or empty", "400", response);
			return;
		}

		builder.append("?redirect_uri=").append(URLEncoder.encode(redirect_uri, StandardCharsets.UTF_8.toString()));
		builder.append("&client_id=").append(client_id);
		builder.append("&scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8.toString()));
		builder.append("&access_type=").append(access_type);
		builder.append("&response_type=").append(response_type);

		if (NotNullEmptyUtility.notNullEmptyCheck(prompt)) {
			prompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8.toString());
			builder.append("&prompt=" + prompt);
		}

		if (NotNullEmptyUtility.notNullEmptyCheck(state)) {
			builder.append("&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8.toString()));
		}

		response.sendRedirect(builder.toString());

	}

	@Override
	public IdTokenModel getAccessTokenFromCode(Map<String, String> requestParameters) {

		if (requestParameters == null || requestParameters.isEmpty()) {
			LOGGER.info("Query parameters are null or empty. No code received.");
			throw new TokenException(400, "Query parameters are null or empty. No code received.");
		}
		requestParameters.forEach((k, v) -> LOGGER.info(k + ":" + v));

		// validate state (exact match)
		String receivedState = requestParameters.get("state");
		if (null == receivedState || !receivedState.equals(state)) {
			LOGGER.info("State mismatch. expected=" + state + " received=" + receivedState);
			throw new TokenException(400, "State mismatch. expected=" + state + " received=" + receivedState);
		}

		// OAuth error from provider
		if (requestParameters.containsKey("error")) {
			LOGGER.info("Authorization server returned error: " + requestParameters.get("error"));
			throw new TokenException(400, "Authorization server returned error: " + requestParameters.get("error"));
		}

		// Missing code from redirect url
		String code = requestParameters.get("code");
		if (!NotNullEmptyUtility.notNullEmptyCheck(code)) {
			LOGGER.info("Authorization code missing.");
			throw new TokenException(400, "Authorization code missing.");

		}

		// exchange code for tokens
		TokenModel tokenModel = exchangeAuthorizationCodeForAccessToken(code);
		if (null == tokenModel) {
			LOGGER.info("tokenModel is null after exchange.");
			throw new TokenException(400, "tokenModel is null after exchange.");
		}

		// check token response errors

		if (NotNullEmptyUtility.notNullEmptyCheck(tokenModel.getError())
				|| NotNullEmptyUtility.notNullEmptyCheck(tokenModel.getError_description())) {
			LOGGER.info("Token endpoint returned error: "
					+ (tokenModel.getError() != null ? tokenModel.getError() : "null") + " / "
					+ (tokenModel.getError_description() != null ? tokenModel.getError_description() : "null"));

			throw new TokenException(400,
					"Token endpoint returned error: " + (tokenModel.getError() != null ? tokenModel.getError() : "null")
							+ " / "
							+ (tokenModel.getError_description() != null ? tokenModel.getError_description() : "null"));
		}

		String idToken = tokenModel.getId_token();

		if (!NotNullEmptyUtility.notNullEmptyCheck(idToken)) {
			LOGGER.info("id_token is missing in token response.");
			throw new TokenException(400, "id_token is missing in token response.");
		}

		IdTokenModel idTokenModel = decodeIdToken(idToken);

		if (null == idTokenModel || !NotNullEmptyUtility.notNullEmptyCheck(idTokenModel.getEmail())) {
			LOGGER.info("Failed to get user info from id_token.");
			throw new TokenException(400, "Failed to get user info from id_token.");

		}

		return idTokenModel;

	}

	public TokenModel exchangeAuthorizationCodeForAccessToken(String authorizationCode) {

		StringBuilder builder = new StringBuilder();
		builder.append("code=").append(URLEncoder.encode(authorizationCode, StandardCharsets.UTF_8)).append("&")
				.append("client_id=").append(URLEncoder.encode(client_id, StandardCharsets.UTF_8)).append("&")
				.append("client_secret=").append(URLEncoder.encode(client_secret, StandardCharsets.UTF_8)).append("&")
				.append("redirect_uri=").append(URLEncoder.encode(redirect_uri, StandardCharsets.UTF_8)).append("&")
				.append("grant_type=authorization_code");
		String requestBody = builder.toString();
		LOGGER.info("request body :" + requestBody);
		HttpClient httpClient = HttpClient.newBuilder().version(Version.HTTP_2).build();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://oauth2.googleapis.com/token"))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();
		try {
			HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			LOGGER.info("response Code:" + httpResponse.statusCode());
			LOGGER.info(httpResponse.body());
			TokenModel tokenModel = ObjectMapperSingleton.INSTANCE.getObjectMapper().readValue(httpResponse.body(),
					TokenModel.class);
			return tokenModel;
		} catch (InterruptedException e) {
			// TODO: handle exception
			Thread.currentThread().interrupt();
			e.printStackTrace();
			throw new TokenException(500, "Interrupted while exchanging token");
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
			throw new TokenException(500, "I/O error while exchanging token");
		}

	}

	private IdTokenModel decodeIdToken(String idToken) {

		String[] parts = idToken.split("\\.");
		if (null != parts && parts.length == 3) {
			String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
			LOGGER.info(payloadJson);
			try {
				return ObjectMapperSingleton.INSTANCE.getObjectMapper().readValue(payloadJson, IdTokenModel.class);
			} catch (IOException e) {
				// TODO: handle exception
				LOGGER.info("Failed to parse id_token payload: " + e.getMessage());
				throw new TokenException(500, "Failed to parse id_token.");
			}

		} else {
			LOGGER.info("Invalid JWT format for id_token.");
			throw new TokenException(400, "Invalid id_token.");
		}

	}

	private void writeErrorResponse(String msg, String statusCode, HttpServletResponse response) throws IOException {

		response.setContentType("application/json");
		response.setStatus(Integer.valueOf(statusCode));
		Map<String, String> responseMap = new HashMap<String, String>();
		responseMap.put("errorMessage", msg);
		responseMap.put("statusCode", statusCode);
		String errorResponse = ObjectMapperSingleton.INSTANCE.getObjectMapper().writeValueAsString(responseMap);
		LOGGER.info("errorResponse:" + errorResponse);
		PrintWriter pw = null;
		try {
			pw = response.getWriter();
			pw.write(errorResponse);
			pw.flush();
		} finally {
			if (null != pw) {
				pw.close();
				LOGGER.info("Closing print writer");

			}
		}

	}

}
