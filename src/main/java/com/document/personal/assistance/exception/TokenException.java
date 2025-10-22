package com.document.personal.assistance.exception;

public class TokenException extends RuntimeException {

	private static final long serialVersionUID = 4492202988365234657L;
	private int statuCode;

	public TokenException() {
		super();
	}

	public TokenException(int statuCode, String message) {
		super(message);
		this.statuCode = statuCode;

	}

	public TokenException(int statuCode, String message, Throwable th) {
		super(message, th);
		this.statuCode = statuCode;
	}

	public TokenException(int statuCode, Throwable th) {
		super(th);
		this.statuCode = statuCode;
	}

	public int getStatuCode() {
		return statuCode;
	}

	public void setStatuCode(int statuCode) {
		this.statuCode = statuCode;
	}

}
