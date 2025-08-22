package com.leetloghub.service;

import com.leetloghub.domain.Member;

import java.util.Optional;

public interface MemberService {
    Optional<Member> findByUsername(String username);

    Member saveGithubAuth(String githubUsername, String accessToken);

    Member saveNotionAuth(String githubUsername, String accessToken);
}