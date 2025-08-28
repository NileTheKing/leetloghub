package com.leethublog.service;


import java.util.List;

public interface GithubService {
    void createRepo(String token, String repoName);
    List<String> getUserRepos(String token);
}
