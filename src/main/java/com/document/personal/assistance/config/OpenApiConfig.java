package com.document.personal.assistance.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI customOpenAPI(@Value("${swagger.server.url}") String serverurl) {

		return new OpenAPI()
				.info(new Info().title("Private Document Assistance").version("1.0.0")
						.description("API documentation for Private Document Assistance backend services")
						.contact(new Contact().name("ANIL LALAM").email("lalamanilbabu@gmail.com")))
				.servers(List.of(
						new Server().url(serverurl)));
	}

}
