package com.leethublog.controller.auth;

import com.leethublog.config.JwtTokenProvider;
import com.leethublog.controller.dto.NotionTokenResponse;
import com.leethublog.service.MemberService;
import com.leethublog.service.NotionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Controller
@RequestMapping("/auth/notion")
@Slf4j
@RequiredArgsConstructor
public class NotionAuthController {

    private final NotionService notionService;
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${spring.security.oauth2.client.registration.notion.client-id}")
    private String notionClientId;

    @Value("${spring.security.oauth2.client.registration.notion.redirect-uri}")
    private String notionRedirectUri;

    @GetMapping("/login")
    public void connectToNotion(@RequestParam("token") String token, HttpServletResponse response) throws IOException {
        if (!jwtTokenProvider.validateToken(token)) {
            log.error("Invalid JWT token provided for Notion connection.");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token");
            return;
        }

        String notionAuthUrl = UriComponentsBuilder
                .fromUriString("https://api.notion.com/v1/oauth/authorize")
                .queryParam("client_id", notionClientId)
                .queryParam("redirect_uri", notionRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("owner", "user")
                // Pass the JWT as the 'state' parameter to retrieve it in the callback
                .queryParam("state", token)
                .build().toUriString();

        response.sendRedirect(notionAuthUrl);
    }

    @GetMapping("/callback")
    public String handleNotionCallback(@RequestParam("code") String code, @RequestParam("state") String token) {
        // The 'state' parameter now contains the original JWT
        if (!jwtTokenProvider.validateToken(token)) {
            log.error("Invalid JWT token received in Notion callback state.");
            return "redirect:/login-failure.html?error=invalid_state";
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(token);
        Long githubId = Long.parseLong(authentication.getName());

        log.info("Handling Notion callback for user with GitHub ID: {}", githubId);

        NotionTokenResponse notionTokenResponse = notionService.requestAccessToken(code);

        if (notionTokenResponse == null) {
            log.error("Failed to retrieve Notion token for user {}", githubId);
            return "redirect:/login-failure.html?error=notion_token_error";
        }

        memberService.saveNotionAuth(githubId, notionTokenResponse.getAccessToken(), notionTokenResponse.getRefreshToken());

        log.info("Successfully linked Notion account for user with GitHub ID: {}", githubId);

        String redirectUrl = UriComponentsBuilder.fromPath("/auth-success.html")
                .queryParam("provider", "notion")
                .queryParam("status", "success")
                .queryParam("workspaceName", notionTokenResponse.getWorkspaceName())
                .toUriString();

        return "redirect:" + redirectUrl;
    }
}
