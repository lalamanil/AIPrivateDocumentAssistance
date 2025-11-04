package com.document.personal.assistance.service;
/**
@author ANIL LALAM
**/
import java.io.IOException;
import java.util.Map;
import com.document.personal.assistance.model.IdTokenModel;
import jakarta.servlet.http.HttpServletResponse;

public interface RedirectGoogleOauth2ServerService {

	public void redirectToAuthorizationURL(HttpServletResponse response) throws IOException;

	public IdTokenModel getAccessTokenFromCode(Map<String, String> requestParameters);

}
