package com.leethublog.controller.api;

import com.leethublog.controller.dto.CreateDbRequestDto;
import com.leethublog.controller.dto.CreateRepoRequestDto;
import com.leethublog.controller.dto.GithubRepoDto;
import com.leethublog.controller.dto.NotionPageDto;
import com.leethublog.controller.dto.SelectRepoRequestDto;
import com.leethublog.service.GithubService;
import com.leethublog.service.NotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config") // New base path for all configuration APIs
@RequiredArgsConstructor
public class ConfigurationController {

    private final GithubService githubService;
    private final NotionService notionService;

    // --- GitHub Endpoints ---

    @GetMapping("/github/repos")
    public ResponseEntity<List<GithubRepoDto>> getGithubRepos(Authentication authentication) {
        return ResponseEntity.ok(githubService.getUserRepos(authentication));
    }

    @PostMapping("/github/repos")
    public ResponseEntity<GithubRepoDto> createAndLinkGithubRepo(@RequestBody CreateRepoRequestDto request, Authentication authentication) {
        GithubRepoDto newRepo = githubService.createAndLinkRepository(request, authentication);
        return new ResponseEntity<>(newRepo, HttpStatus.CREATED);
    }

    @PutMapping("/github/target-repository")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void linkGithubRepo(@RequestBody SelectRepoRequestDto request, Authentication authentication) {
        githubService.linkRepository(request.getRepoFullName(), authentication);
    }

    // --- Notion Endpoints ---

    @GetMapping("/notion/pages")
    public ResponseEntity<List<NotionPageDto>> getNotionPages(Authentication authentication) {
        return ResponseEntity.ok(notionService.getAvailablePages(authentication));
    }

    @PostMapping("/notion/databases")
    public ResponseEntity<Void> createNotionDatabase(@RequestBody CreateDbRequestDto request, Authentication authentication) {
        notionService.createDatabase(request.getPageId(), authentication);
        return ResponseEntity.ok().build();
    }
}
