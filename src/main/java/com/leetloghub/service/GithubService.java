package com.leetloghub.service;

public interface GithubService {
    void commitCode(String token, String repo, String filePath, String content, String commitMessage);
}
