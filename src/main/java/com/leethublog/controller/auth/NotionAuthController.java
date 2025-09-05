package com.leethublog.controller.auth;

import com.leethublog.controller.dto.NotionTokenResponse;
import com.leethublog.service.MemberService;
import com.leethublog.service.NotionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @GetMapping("/login")
    public void connectToNotion(Authentication authentication, HttpSession session, HttpServletResponse response) throws IOException {
        String githubId = authentication.getName();
        log.info("Starting Notion connection for user with GitHub ID: {}", githubId);
        session.setAttribute("githubIdToLink", githubId);
        response.sendRedirect("/oauth2/authorization/notion");
    }

    @GetMapping("/callback")
    public String handleNotionCallback(@RequestParam("code") String code, HttpSession session) {
        String githubIdStr = (String) session.getAttribute("githubIdToLink");
        if (githubIdStr == null) {
            log.error("Cannot find GitHub ID in session. Notion linking failed.");
            return "redirect:/login-failure.html?error=session_expired";
        }
        session.removeAttribute("githubIdToLink");

        Long githubId = Long.parseLong(githubIdStr);
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