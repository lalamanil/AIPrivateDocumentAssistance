package com.document.personal.assistance.service;

import java.util.Map;

public interface GcsResumableUpload {

	public Map<String, String> initiateResumableUploadSessionUri(String objectName, String contentType);

	public Map<String, String> uploadChunk(String sessionUrl, String contentRange, String contentLength, byte[] chunk);

}
