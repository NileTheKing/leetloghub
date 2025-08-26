package com.leethublog.service;

import java.util.List;

public interface GithubService {
    void commitCode(String token, String repo, String filePath, String content, String commitMessage);
    List<String> getRepositories(String token);
    void createRepository(String token, String repoName);


    String requestAccessToken(String code);
    String getGithubUsername(String accessToken);
}
