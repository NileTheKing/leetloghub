package com.leetloghub.controller.auth;

import com.leetloghub.controller.dto.GithubTokenResponse;
import com.leetloghub.controller.dto.GithubUserResponse;
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

@Controller
@RequestMapping("/auth/github")
public class GithubAuthController {

    private final MemberService memberService;

    @Value("${github.client.id}")
    private String clientId;
    @Value("${github.client.secret}")
    private String clientSecret;
    @Value("${github.url.authorize}")
    private String authorizeUrl;
    @Value("${github.url.token}")
    private String tokenUrl;
    @Value("${github.url.user}")
    private String userUrl;

    public GithubAuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/login")
    public void redirectToGithub(HttpServletResponse response) throws IOException {
        String location = authorizeUrl + "?client_id=" + clientId + "&scope=repo,user";
        response.sendRedirect(location);
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleGithubCallback(@RequestParam("code") String code) {
        String accessToken = requestAccessToken(code);
        String githubUsername = getGithubUsername(accessToken);

        memberService.saveGithubAuth(githubUsername, accessToken);

        return ResponseEntity.ok("GitHub authentication successful for user: " + githubUsername);
    }

    private String requestAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/json");
        String body = String.format("client_id=%s&client_secret=%s&code=%s", clientId, clientSecret, code);
        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<GithubTokenResponse> responseEntity = restTemplate.exchange(tokenUrl, HttpMethod.POST, requestEntity, GithubTokenResponse.class);
        if (responseEntity.getBody() != null) {
            return responseEntity.getBody().getAccessToken();
        }
        throw new RuntimeException("Failed to retrieve access token from GitHub.");
    }

    private String getGithubUsername(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<GithubUserResponse> responseEntity = restTemplate.exchange(userUrl, HttpMethod.GET, requestEntity, GithubUserResponse.class);
        if (responseEntity.getBody() != null) {
            return responseEntity.getBody().getLogin();
        }
        throw new RuntimeException("Failed to retrieve user info from GitHub.");
    }
}
