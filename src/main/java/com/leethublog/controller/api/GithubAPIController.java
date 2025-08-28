package com.leethublog.controller.api;

import com.leethublog.controller.dto.GithubRepoDto;
import com.leethublog.service.GithubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GithubAPIController {

    private final GithubService githubService;

    @GetMapping("/repos") // Use plural for collections
    public ResponseEntity<List<GithubRepoDto>> getRepos(Authentication authentication) {
        // The controller's job is simple: delegate to the service.
        List<GithubRepoDto> userRepos = githubService.getUserRepos(authentication);
        return ResponseEntity.ok(userRepos);
    }

    @PostMapping("/repos")
    public void createRepo() {
        // TODO: Implement repository creation
    }
}
