package com.aqua.plus.api.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class UtilsRestemplate {

	@Value("${alegra.token}")
	private String token;
	
	public HttpHeaders getHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + token);
		headers.setContentType(MediaType.APPLICATION_JSON);

		return headers;
	}
}
