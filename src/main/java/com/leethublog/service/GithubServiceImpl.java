package com.leethublog.service;

import com.leethublog.controller.dto.GithubTokenResponse;
import com.leethublog.controller.dto.GithubUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class GithubServiceImpl implements GithubService {

    private final MemberService memberService;
    private final String clientId;
    private final String clientSecret;

    public GithubServiceImpl(MemberService memberService,
                             @Value("${github.client.id}") String clientId,
                             @Value("${github.client.secret}") String clientSecret) {
        this.memberService = memberService;
        this.clientId = clientId;
        this.clientSecret = clientSecret;


    }

    @Override
    public void commitCode(String token, String repo, String filePath, String content, String commitMessage) {
        // TODO: Implement GitHub API call using a library like org.kohsuke.github-api
        System.out.println("Committing to GitHub: " + filePath);
    }

    @Override
    public List<String> getRepositories(String token) {
        return List.of();
    }

    @Override
    public void createRepository(String token, String repoName) {

    }

    @Override
    public String requestAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/json");
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        String url = "https://github.com/login/oauth/access_token";
        String body = String.format("client_id=%s&client_secret=%s&code=%s", clientId, clientSecret, code);
        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<GithubTokenResponse> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, GithubTokenResponse.class);
        if (responseEntity.getBody() != null) {
            return responseEntity.getBody().getAccessToken();
        }
        throw new RuntimeException("Failed to retrieve access token from GitHub.");
    }

    @Override
    public String getGithubUsername(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<GithubUserResponse> responseEntity = restTemplate.exchange("https://api.github.com/user", HttpMethod.GET, requestEntity, GithubUserResponse.class);
        if (responseEntity.getBody() != null) {
            return responseEntity.getBody().getLogin();
        }
        throw new RuntimeException("Failed to retrieve user info from GitHub.");
    }
}