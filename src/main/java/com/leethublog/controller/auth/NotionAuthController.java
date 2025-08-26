package com.leethublog.controller.auth;

import com.leethublog.service.MemberService;
import com.leethublog.service.NotionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Optional;

@Controller
@RequestMapping("/auth/notion")
@Slf4j
public class NotionAuthController {

    private final MemberService memberService;
    private final NotionService notionService;
    private final String clientId;
    private final String clientSecret;

    public NotionAuthController(MemberService memberService, NotionService notionService,
                                @Value("${notion.client.id}") String clientId,
                                @Value("${notion.client.secret}") String clientSecret) {
        this.memberService = memberService;
        this.notionService = notionService;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private final String redirectUri = "http://localhost:8080/auth/notion/callback";


    @GetMapping("/login")
    public void redirectToNotion(HttpServletResponse response, Authentication authentication,
                                 HttpSession httpSession) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()) {
            if (authentication == null) {
                log.info("authentication is null");
            } else {
                log.info("authentication is not authenticated");
            }
        }
        String githubUsername = Optional.ofNullable(authentication).map(Authentication::getName).orElse("unknown");
        log.info("Redirecting to Notion for OAuth2 authentication for GitHub user: {}", githubUsername);
        httpSession.setAttribute("githubUsername", githubUsername);

        String location = "https://api.notion.com/v1/oauth/authorize?client_id=" + clientId + "&response_type=code&owner=user&redirect_uri=" + redirectUri;
        response.sendRedirect(location);
    }

    @GetMapping("/callback")
    public String handleNotionCallback(@RequestParam("code") String code , HttpSession httpSession) {


        String githubUsername = (String) httpSession.getAttribute("githubUsername");
        if (githubUsername == null) {
            throw new IllegalStateException("GitHub username not found in session");
        }
        String notionAccessToken = notionService.requestAccessToken(code);
        memberService.saveNotionAuth(githubUsername, notionAccessToken);
        httpSession.removeAttribute("githubUsername");


        return "redirect:/auth-success.html?user=" + githubUsername;
    }


}