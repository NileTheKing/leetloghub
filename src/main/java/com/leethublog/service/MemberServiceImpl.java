package com.leethublog.service;

import com.leethublog.domain.Member;
import com.leethublog.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final EncryptionService encryptionService;

    public MemberServiceImpl(MemberRepository memberRepository, EncryptionService encryptionService) {
        this.memberRepository = memberRepository;
        this.encryptionService = encryptionService;
    }

    @Override
    public Optional<Member> findByUsername(String username) {
        return memberRepository.findByGithubUsername(username);
    }

    @Transactional
    public Member saveGithubAuth(String githubUsername, String accessToken) {
        Optional<Member> existingMember = memberRepository.findByGithubUsername(githubUsername);

        Member member = existingMember.orElseGet(Member::new);
        if (member.getGithubUsername() == null) {
            member.setGithubUsername(githubUsername);
        }
        
        String encryptedToken = encryptionService.encrypt(accessToken);
        member.setEncryptedGithubToken(encryptedToken);

        return memberRepository.save(member);
    }

    @Transactional
    public Member saveNotionAuth(String githubUsername, String accessToken) {
        Member member = memberRepository.findByGithubUsername(githubUsername)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot save Notion token without an existing GitHub authentication."));
        
        String encryptedToken = encryptionService.encrypt(accessToken);
        member.setEncryptedNotionToken(encryptedToken);

        return memberRepository.save(member);
    }
}