package com.leethublog.service;

import com.leethublog.controller.dto.NotionSyncDto;
import com.leethublog.controller.dto.SolveRequestDto;
import com.leethublog.domain.*;
import com.leethublog.repository.MemberRepository;
import com.leethublog.repository.ProblemStatusRepository;
import com.leethublog.repository.SolveLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SolveServiceImpl implements SolveService {

    private final MemberRepository memberRepository;
    private final SolveLogRepository solveLogRepository;
    private final ProblemStatusRepository problemStatusRepository;
    private final NotionService notionService;
    private final GithubService githubService; // Add GithubService dependency

    @Override
    @Transactional
    public void processNewSolve(Authentication authentication, SolveRequestDto solveRequest) {
        // 1. Find Member
        Long githubId = Long.parseLong(authentication.getName());
        Member member = memberRepository.findByGithubId(githubId)
                .orElseThrow(() -> new IllegalStateException("Member not found: " + githubId));

        // 2. Save the new SolveLog
        SolveLog savedLog = saveNewSolveLog(member, solveRequest);

        // 3. Find or Create the ProblemStatus for this user and problem
        ProblemStatus status = problemStatusRepository.findByMemberAndProblemUrl(member, solveRequest.getProblemUrl())
                .orElse(new ProblemStatus(member, solveRequest.getProblemUrl()));

        // 4. Get full history to pass to the rules engine
        List<SolveLog> history = solveLogRepository.findByMemberAndProblemUrlOrderBySolvedAtAsc(member, solveRequest.getProblemUrl());

        // 5. Apply the SRS Rules Engine
        updateStatusBasedOnRules(status, history, solveRequest.getSolveStatus());

        // 6. Save the updated status
        problemStatusRepository.save(status);
        log.info("Updated ProblemStatus for user: {}, problem: {}. New interval: {}, Next review: {}",
            member.getGithubLogin(), solveRequest.getProblemTitle(), status.getCurrentInterval(), status.getNextReviewDate());

        // 7. Build DTO for Notion Sync
        NotionSyncDto syncDto = buildNotionSyncDto(solveRequest, status, history, savedLog);

        // 8. Sync to Notion
        notionService.syncSolveToNotion(member, syncDto);

        // 9. Upload solution files to GitHub
        githubService.uploadSolutionFiles(authentication, solveRequest);
    }

    private SolveLog saveNewSolveLog(Member member, SolveRequestDto solveRequest) {
        SolveLog newSolveLog = new SolveLog();
        newSolveLog.setMember(member);
        newSolveLog.setProblemTitle(solveRequest.getProblemTitle());
        newSolveLog.setProblemUrl(solveRequest.getProblemUrl());
        newSolveLog.setProblemDifficulty(solveRequest.getProblemDifficulty());
        newSolveLog.setSolveStatus(solveRequest.getSolveStatus());
        newSolveLog.setCode(solveRequest.getCode());
        return solveLogRepository.save(newSolveLog);
    }

    private void updateStatusBasedOnRules(ProblemStatus status, List<SolveLog> history, SolveStatus currentSolveStatus) {
        int attempts = history.size();
        int lastInterval = status.getCurrentInterval();
        int newInterval = 0;

        if (attempts == 1) { // First time solving
            if (currentSolveStatus == SolveStatus.PERFECT) {
                status.setReviewStatus(ReviewStatus.MASTERED);
                status.setNextReviewDate(null);
                status.setCurrentInterval(0);
                return; // Exit early
            } else if (currentSolveStatus == SolveStatus.SOLUTION || currentSolveStatus == SolveStatus.HINT) {
                newInterval = 1;
            } else { // STRUGGLED or GOOD
                newInterval = 7;
            }
        } else { // Subsequent solves
            SolveStatus previousSolveStatus = history.get(history.size() - 2).getSolveStatus();
            boolean isImprovement = currentSolveStatus.getProficiency() > previousSolveStatus.getProficiency();

            if (isImprovement) {
                switch (lastInterval) {
                    case 0: // This can happen if a previously mastered problem is re-solved
                    case 1:
                        newInterval = 7;
                        break;
                    case 7:
                        newInterval = 28;
                        break;
                    case 28:
                        if (currentSolveStatus == SolveStatus.PERFECT) {
                            status.setReviewStatus(ReviewStatus.MASTERED);
                            status.setNextReviewDate(null);
                            status.setCurrentInterval(0);
                            return; // Exit early
                        }
                        newInterval = 28; // Stay at max interval if not perfect
                        break;
                    default:
                        newInterval = 1; // Default case
                        break;
                }
            } else { // Stagnation or Regression
                newInterval = lastInterval == 0 ? 1 : lastInterval; // If it was mastered, restart from 1 day
            }
        }

        status.setCurrentInterval(newInterval);
        status.setNextReviewDate(LocalDate.now().plusDays(newInterval));
        status.setReviewStatus(ReviewStatus.REVIEWING);
    }

    private NotionSyncDto buildNotionSyncDto(SolveRequestDto solveRequest, ProblemStatus status, List<SolveLog> history, SolveLog savedLog) {
        String historySummary = history.stream()
            .map(log -> log.getSolveStatus().name().substring(0, 1)) // e.g., P, G, S, H, S
            .collect(Collectors.joining(" -> "));

        return NotionSyncDto.builder()
                .problemTitle(solveRequest.getProblemTitle())
                .problemUrl(solveRequest.getProblemUrl())
                .problemDifficulty(solveRequest.getProblemDifficulty())
                .lastSolved(savedLog.getSolvedAt().toLocalDate())
                .nextReview(status.getNextReviewDate())
                .attempts(history.size())
                .solveStatus(solveRequest.getSolveStatus())
                .reviewStatus(status.getReviewStatus())
                .historySummary(historySummary)
                .build();
    }
}
