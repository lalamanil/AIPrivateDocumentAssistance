package com.document.personal.assistance.utility;
/**
@author ANIL LALAM
**/
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;

public class TextToSpeechUtility {
	private static final Logger LOGGER = Logger.getLogger(TextToSpeechUtility.class.getName());
	private static TextToSpeechClient textToSpeechClient;
	static {
		InputStream inputStream = TextToSpeechUtility.class.getClassLoader()
				.getResourceAsStream("AI-ServiceAccount.json");
		if (null == inputStream) {
			LOGGER.info(
					"Please check service account with name AI-ServiceAccount.json is present under src/main/resources. Inputstream is null");
		} else {
			try {
				GoogleCredentials googleCredentials = GoogleCredentials.fromStream(inputStream)
						.createScoped("https://www.googleapis.com/auth/cloud-platform");
				TextToSpeechSettings textToSpeechSettings = TextToSpeechSettings.newBuilder()
						.setCredentialsProvider(() -> googleCredentials).build();
				textToSpeechClient = TextToSpeechClient.create(textToSpeechSettings);
			} catch (IOException e) {
				// TODO: handle exception
				e.printStackTrace();
			}

		}

	}
	public static byte[] convertTextToSpeech(String summaryText, String ttsLanguageCode) {
		if (null == textToSpeechClient) {
			LOGGER.info("textToSpeechClient is null. Please check application logs");
			return null;
		}
		if (!NotNullEmptyUtility.notNullEmptyCheck(summaryText)
				|| !NotNullEmptyUtility.notNullEmptyCheck(ttsLanguageCode)) {
			LOGGER.info("summaryText or ttsLanguageCode cannot be null or empty. Please check application logs");
			return null;
		}
		SynthesisInput synthesisInput = SynthesisInput.newBuilder().setText(summaryText).build();
		// configuring voice type and language
		VoiceSelectionParams voiceSelectionParams = VoiceSelectionParams.newBuilder().setLanguageCode(ttsLanguageCode)
				.setSsmlGender(SsmlVoiceGender.FEMALE).build();
		AudioConfig audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.MP3).build();
		try {
			SynthesizeSpeechResponse synthesizeSpeechResponse = textToSpeechClient.synthesizeSpeech(synthesisInput,
					voiceSelectionParams, audioConfig);
			ByteString audioContent = synthesizeSpeechResponse.getAudioContent();
			if (null != audioContent) {
				return audioContent.toByteArray();
			}
		} catch (PermissionDeniedException e) {
			// TODO: handle exception
			String message = e.getMessage();
			if (message.contains("has not been used in project") || message.contains("it is disabled")) {
				LOGGER.info("❌ Cloud Text-to-Speech API is **not enabled** for this project. Please enable it at:\n"
						+ "👉 https://console.developers.google.com/apis/api/texttospeech.googleapis.com/overview?project=");
			} else {
				LOGGER.info("❗ Permission Denied: " + message);
				LOGGER.info("❗ Unrecognized permission error. Please check IAM and API status.");

			}
		} catch (ApiException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}

}
