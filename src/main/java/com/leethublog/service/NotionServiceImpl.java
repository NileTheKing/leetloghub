package com.leethublog.service;

import com.leethublog.controller.dto.GithubTokenResponse;
import com.leethublog.controller.dto.GithubUserResponse;
import com.leethublog.controller.dto.NotionTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class NotionServiceImpl implements NotionService {

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public NotionServiceImpl(
            @Value("${notion.client.id}")String clientId,
            @Value("${notion.client.secret}")String clientSecret,
            @Value("${notion.client.callback}")String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }
    @Override
    public void createPageInDatabase(String token, String databaseId, String title) {
        // TODO: Implement Notion API call using a library like rest-api-client
        // 1. Build API request with authentication token
        // 2. Set database_id and page properties (like title)
        // 3. Send POST request to /v1/pages
        System.out.println("Creating Notion page with title: " + title);
    }
    @Override
    public String requestAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();

        log.info("Requesting Notion access token with code: {}", code);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Notion-Version", "2022-06-28");

        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        body.put("redirect_uri", redirectUri);

        HttpEntity<Map<String,String>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<NotionTokenResponse> responseEntity = restTemplate.exchange(
                "https://api.notion.com/v1/oauth/token",
                HttpMethod.POST,
                requestEntity,
                NotionTokenResponse.class
        );

        if (responseEntity.getBody() != null) {
            return responseEntity.getBody().getAccessToken();
        } else {
            throw new RuntimeException("Failed to retrieve Notion access token");
        }

    }


}
