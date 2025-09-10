package com.leethublog.repository;

import com.leethublog.domain.Member;
import com.leethublog.domain.SolveLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolveLogRepository extends JpaRepository<SolveLog, Long> {
    List<SolveLog> findByMemberAndProblemUrlOrderBySolvedAtAsc(Member member, String problemUrl);
}
