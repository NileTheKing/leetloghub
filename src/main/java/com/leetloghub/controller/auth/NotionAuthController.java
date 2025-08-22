package com.leetloghub.controller.auth;

import com.leetloghub.controller.dto.NotionTokenResponse;
import com.leetloghub.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Controller
@RequestMapping("/auth/notion")
public class NotionAuthController {

    private final MemberService memberService;

    @Value("${notion.client.id}")
    private String clientId;

    @Value("${notion.client.secret}")
    private String clientSecret;

    private final String redirectUri = "http://localhost:8080/auth/notion/callback";

    public NotionAuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/login")
    public void redirectToNotion(@RequestParam("user") String githubUsername, HttpServletResponse response) throws IOException {
        String location = "https://api.notion.com/v1/oauth/authorize?" +
                "client_id=" + clientId +
                "&response_type=code" +
                "&owner=user" +
                "&redirect_uri=" + redirectUri +
                "&state=" + githubUsername;
        response.sendRedirect(location);
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleNotionCallback(@RequestParam("code") String code, @RequestParam("state") String githubUsername) {
        String accessToken = requestAccessToken(code);

        memberService.saveNotionAuth(githubUsername, accessToken);

        return ResponseEntity.ok("Notion authentication successful for user: " + githubUsername);
    }

    private String requestAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        String auth = clientId + ":" + clientSecret;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new java.util.HashMap<>();
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        body.put("redirect_uri", redirectUri);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<NotionTokenResponse> responseEntity = restTemplate.exchange(
                "https://api.notion.com/v1/oauth/token",
                HttpMethod.POST,
                requestEntity,
                NotionTokenResponse.class
        );

        if (responseEntity.getBody() != null) {
            return responseEntity.getBody().getAccessToken();
        }
        throw new RuntimeException("Failed to retrieve access token from Notion.");
    }
}