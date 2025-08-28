package com.leethublog.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class GithubServiceImpl implements GithubService {
    @Override
    public void createRepo(String token, String repoName) {
        RestTemplate restTemplate = new RestTemplate();

    }

    @Override
    public List<String> getUserRepos(String token) {

        return null;
    }
}
