package com.leethublog.controller.auth;

import com.leethublog.service.GithubService;
import com.leethublog.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
@RequestMapping("/auth/github")
@Slf4j
public class GithubAuthController {

    private final MemberService memberService;
    private final GithubService githubService;
    private final String clientId;

    public GithubAuthController(MemberService memberService,
                              GithubService githubService,
                              @Value("${github.client.id}") String clientId) {
        this.memberService = memberService;
        this.githubService = githubService;
        this.clientId = clientId;
    }

    @GetMapping("/login")
    public void redirectToGithub(HttpServletResponse response) throws IOException {
        String location = "https://github.com/login/oauth/authorize?client_id=" + clientId + "&scope=repo,user";
        response.sendRedirect(location);
    }

    @GetMapping("/callback")
    public String handleGithubCallback(@RequestParam("code") String code) {
        String accessToken = githubService.requestAccessToken(code);
        String githubUsername = githubService.getGithubUsername(accessToken);

        memberService.saveGithubAuth(githubUsername, accessToken);
        log.info("GitHub authentication successful for user: {}", githubUsername);
        return "redirect:/auth-success.html?user=" + githubUsername;
    }
}
