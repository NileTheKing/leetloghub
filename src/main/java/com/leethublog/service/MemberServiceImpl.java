package com.leethublog.service;

import com.leethublog.domain.Member;
import com.leethublog.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final EncryptionService encryptionService;

    @Transactional
    @Override
    public Member saveGithubAuth(Long githubId, String githubLogin, String accessToken) {
        Member member = memberRepository.findByGithubId(githubId).orElseGet(Member::new);
        member.setGithubId(githubId);
        member.setGithubLogin(githubLogin);
        member.setEncryptedGithubToken(encryptionService.encrypt(accessToken));
        return memberRepository.save(member);
    }

    @Transactional
    @Override
    public Member saveNotionAuth(Long githubId, String accessToken, String refreshToken) {
        // Find user by their immutable GitHub ID
        Member member = memberRepository.findByGithubId(githubId)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot save Notion token for a user not registered with GitHub. GitHub ID: " + githubId));

        String encryptedAccessToken = encryptionService.encrypt(accessToken);
        member.setEncryptedNotionToken(encryptedAccessToken);

        if (refreshToken != null) {
            String encryptedRefreshToken = encryptionService.encrypt(refreshToken);
            member.setEncryptedNotionRefreshToken(encryptedRefreshToken);
        }

        return memberRepository.save(member);
    }
}