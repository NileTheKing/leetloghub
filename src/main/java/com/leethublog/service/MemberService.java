package com.leethublog.service;

import com.leethublog.domain.Member;

import java.util.Optional;

public interface MemberService {
    Optional<Member> findByUsername(String username);

    Member saveGithubAuth(String githubUsername, String accessToken);

    Member saveNotionAuth(String githubUsername, String accessToken);
}