package com.leethublog.service;

import com.leethublog.domain.Member;
import com.leethublog.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
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
        // Find member by the immutable githubId
        Optional<Member> existingMember = memberRepository.findByGithubId(githubId);

        Member member = existingMember.orElseGet(Member::new);

        // Set or update member details
        member.setGithubId(githubId);
        member.setGithubLogin(githubLogin); // Update login name in case it has changed

        String encryptedToken = encryptionService.encrypt(accessToken);
        member.setEncryptedGithubToken(encryptedToken);

        return memberRepository.save(member);
    }

    @Transactional
    @Override
    public Member saveNotionAuth(Long githubId, String accessToken, String refreshToken, String databaseId) {
        // Find user by their immutable GitHub ID
        Member member = memberRepository.findByGithubId(githubId)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot save Notion token for a user not registered with GitHub. GitHub ID: " + githubId));

        String encryptedAccessToken = encryptionService.encrypt(accessToken);
        member.setEncryptedNotionToken(encryptedAccessToken);
        member.setTargetDbId(databaseId); // Save the Notion Database ID

        if (refreshToken != null) {
            String encryptedRefreshToken = encryptionService.encrypt(refreshToken);
            member.setEncryptedNotionRefreshToken(encryptedRefreshToken);
        }

        return memberRepository.save(member);
    }
}