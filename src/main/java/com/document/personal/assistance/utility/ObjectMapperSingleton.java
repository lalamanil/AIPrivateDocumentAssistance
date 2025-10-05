package com.document.personal.assistance.utility;

import com.fasterxml.jackson.databind.ObjectMapper;

public enum ObjectMapperSingleton {
	INSTANCE;

	private final ObjectMapper objectMapper;

	ObjectMapperSingleton() {
		// TODO Auto-generated constructor stub
		objectMapper = new ObjectMapper();
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

}
