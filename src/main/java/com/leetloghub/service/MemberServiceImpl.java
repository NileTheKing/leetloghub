package com.leetloghub.service;

import com.leetloghub.domain.Member;
import com.leetloghub.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    public MemberServiceImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public Optional<Member> findByUsername(String username) {
        // Implementation will follow in a subsequent step
        return Optional.empty();
    }
}
