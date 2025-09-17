package com.leethublog.service;

import com.leethublog.controller.dto.CreateRepoRequestDto;
import com.leethublog.controller.dto.GithubRepoDto;
import com.leethublog.controller.dto.SolveRequestDto;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface GithubService {

    List<GithubRepoDto> getUserRepos(Authentication authentication);

    void linkRepository(String repoFullName, Authentication authentication);

    GithubRepoDto createAndLinkRepository(CreateRepoRequestDto request, Authentication authentication);

    void uploadSolutionFiles(Authentication authentication, SolveRequestDto solveInfo);

}
