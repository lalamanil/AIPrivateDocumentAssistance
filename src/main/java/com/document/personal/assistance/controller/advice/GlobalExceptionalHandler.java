package com.document.personal.assistance.controller.advice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.document.personal.assistance.exception.PrivateDocumentException;
import com.document.personal.assistance.model.PrivateDocumentErrorModel;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionalHandler {

	@ExceptionHandler(PrivateDocumentException.class)
	@ResponseBody
	public ResponseEntity<PrivateDocumentErrorModel> handleCustomRuntimeExceptions(PrivateDocumentException ex,
			HttpServletRequest request) {
		PrivateDocumentErrorModel privateDocumentErrorModel = new PrivateDocumentErrorModel(ex.getStatusCode(),
				ex.getMessage(), request.getRequestURI(), Instant.now());
		return ResponseEntity.status(HttpStatus.valueOf(ex.getStatusCode())).body(privateDocumentErrorModel);

	}

	@ExceptionHandler({ MethodArgumentNotValidException.class, MissingServletRequestParameterException.class,
			MissingRequestHeaderException.class, HandlerMethodValidationException.class })
	@ResponseBody
	public ResponseEntity<PrivateDocumentErrorModel> handleBadRequestExceptions(Exception ex,
			HttpServletRequest request) {
		String message;
		if (ex instanceof MissingRequestHeaderException headerex) {
			message = "Required header '" + headerex.getHeaderName() + "' is missing";
		} else {
			if (ex instanceof MethodArgumentNotValidException valex) {
				message = valex.getBindingResult().getFieldErrors().stream()
						.map(err -> err.getField() + " " + err.getDefaultMessage()).collect(Collectors.joining(","));
			} else {
				if (ex instanceof MissingServletRequestParameterException missex) {
					message = "Required query parameter '" + missex.getParameterName() + "' of type '"
							+ missex.getParameterType() + "' is missing";
				} else {
					message = ex.getMessage();
				}
			}
		}
		PrivateDocumentErrorModel errorModel = new PrivateDocumentErrorModel(HttpStatus.BAD_REQUEST.value(), message,
				request.getRequestURI(), Instant.now());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorModel);
	}

	@ExceptionHandler(Exception.class)
	@ResponseBody
	public ResponseEntity<PrivateDocumentErrorModel> handleException(Exception ex, HttpServletRequest request) {

		ex.printStackTrace();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new PrivateDocumentErrorModel(500, ex.getMessage(), request.getRequestURI(), Instant.now()));

	}

}
