package com.leethublog.controller.auth;

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

import java.io.IOException;

@Controller
@RequestMapping("/auth/notion")
@Slf4j
@RequiredArgsConstructor
public class NotionAuthController {

    private final NotionService notionService;
    private final MemberService memberService;

    // Starts the Notion account linking process
    @GetMapping("/login")
    public void connectToNotion(Authentication authentication, HttpSession session, HttpServletResponse response) throws IOException {
        // The user must be authenticated with GitHub at this point
        String githubId = authentication.getName();
        log.info("Starting Notion connection for user with GitHub ID: {}", githubId);

        // Store the primary user's identifier in the session to link accounts on callback
        session.setAttribute("githubIdToLink", githubId);

        // Redirect to the standard Spring Security endpoint to initiate the Notion OAuth2 flow
        // Spring will use the properties in application.properties to build the correct Notion URL
        response.sendRedirect("/oauth2/authorization/notion");
    }

    // This is the callback endpoint that Notion redirects to.
    // The path matches the redirect-uri in application.properties
    @GetMapping("/callback")
    public String handleNotionCallback(@RequestParam("code") String code, HttpSession session) {
        String githubIdStr = (String) session.getAttribute("githubIdToLink");
        if (githubIdStr == null) {
            log.error("Cannot find GitHub ID in session. Notion linking failed.");
            return "redirect:/login-failure.html?error=session_expired";
        }
        session.removeAttribute("githubIdToLink"); // Clean up session

        Long githubId = Long.parseLong(githubIdStr);
        log.info("Handling Notion callback for user with GitHub ID: {}", githubId);

        // Use the refactored NotionService to get the token
        String notionAccessToken = notionService.requestAccessToken(code);

        // Save the new Notion token, associating it with the user's GitHub ID
        memberService.saveNotionAuth(githubId, notionAccessToken);

        log.info("Successfully linked Notion account for user with GitHub ID: {}", githubId);
        return "redirect:/auth-success.html"; // Redirect to a generic success page
    }
}
