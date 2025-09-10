package com.leethublog.controller.dto;

import com.leethublog.domain.Difficulty;
import com.leethublog.domain.ReviewStatus;
import com.leethublog.domain.SolveStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class NotionSyncDto {
    // Identifying Info
    private String problemTitle;
    private String problemUrl;
    private Difficulty problemDifficulty;

    // SRS Calculated Data
    private LocalDate lastSolved;
    private LocalDate nextReview;
    private Integer attempts;
    private SolveStatus solveStatus;
    private ReviewStatus reviewStatus;
    private String historySummary;
}
