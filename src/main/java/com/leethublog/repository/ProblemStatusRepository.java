package com.leethublog.repository;

import com.leethublog.domain.Member;
import com.leethublog.domain.ProblemStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProblemStatusRepository extends JpaRepository<ProblemStatus, Long> {
    Optional<ProblemStatus> findByMemberAndProblemUrl(Member member, String problemUrl);
}
