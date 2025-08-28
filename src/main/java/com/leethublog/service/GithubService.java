package com.leethublog.service;

import com.leethublog.controller.dto.GithubRepoDto;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface GithubService {
    void createRepo(String token, String repoName);
    List<GithubRepoDto> getUserRepos(Authentication authentication);
}
