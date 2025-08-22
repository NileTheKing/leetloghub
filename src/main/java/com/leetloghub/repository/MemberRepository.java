package com.leetloghub.repository;

import com.leetloghub.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByGithubUsername(String githubUsername);
}
