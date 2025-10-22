package com.document.personal.assistance.utility;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.document.personal.assistance.exception.PrivateDocumentException;
import com.google.auth.oauth2.GoogleCredentials;

public class AccessTokenUtility {

	private static final Logger LOGGER = Logger.getLogger(AccessTokenUtility.class.getName());

	private static final Object LOCK = new Object();

	private static GoogleCredentials credentials;

	static {
		try {
			InputStream inputStream = AccessTokenUtility.class.getClassLoader()
					.getResourceAsStream("AI-ServiceAccount.json");

			if (null == inputStream) {
				String msg = "Service account file not found in classpath:AI-ServiceAccount.json";
				throw new PrivateDocumentException(msg, 500);
			}
			credentials = GoogleCredentials.fromStream(inputStream)
					.createScoped("https://www.googleapis.com/auth/cloud-platform");

			LOGGER.info("GoogleCredentials initialized successfully.");
		} catch (IOException e) {
			// TODO: handle exception
			String msg = "Failed to initialize GoogleCredentials: " + e.getMessage();
			LOGGER.log(Level.SEVERE, msg, e);
			throw new PrivateDocumentException(msg, 500);
		}
	}

	public static String getAccessToken() {
		if (null == credentials) {
			String msg = "Google credentials not initialized. Cannot retrieve access token.";
			throw new PrivateDocumentException(msg, 500);
		}
		try {

			// First Check: non-blocking, fast path
			if (isTokenExpiredOrAboutToExpire()) {

				synchronized (LOCK) {

					if (isTokenExpiredOrAboutToExpire()) {
						credentials.refreshIfExpired();
					}

				}

			}

			String accessToken = credentials.getAccessToken().getTokenValue();
			LOGGER.info("accessToken:" + accessToken);
			return accessToken;
		} catch (IOException ex) {

			String msg = "I/O exception while refreshing access token: " + ex.getMessage();

			LOGGER.log(Level.SEVERE, msg, ex);
			throw new PrivateDocumentException(msg, 500);

		}

	}

	private static boolean isTokenExpiredOrAboutToExpire() {

		return null == credentials.getAccessToken() || null == credentials.getAccessToken().getExpirationTime()
				|| Instant.now().isAfter(credentials.getAccessToken().getExpirationTime().toInstant().minusSeconds(60));
	}

}
