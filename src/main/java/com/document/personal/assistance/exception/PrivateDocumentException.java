package com.document.personal.assistance.exception;
/**
@author ANIL LALAM
**/
public class PrivateDocumentException extends RuntimeException {

	private static final long serialVersionUID = 3996654351417178201L;

	private int statusCode;

	public PrivateDocumentException(String msg, int statusCode) {
		super(msg);
		this.statusCode = statusCode;

	}

	public PrivateDocumentException(String msg, Throwable th, int statusCode) {
		super(msg, th);
		this.statusCode = statusCode;
	}

	public PrivateDocumentException(Throwable th, int statusCode) {
		super(th);
		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

}
