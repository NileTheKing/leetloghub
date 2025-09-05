package com.leethublog.service;

import com.leethublog.domain.Member;

import java.util.Optional;

public interface MemberService {

    Member saveGithubAuth(Long githubId, String githubLogin, String accessToken);

    Member saveNotionAuth(Long githubId, String accessToken, String refreshToken);
}