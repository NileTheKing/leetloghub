package com.leethublog.controller.dto;

import com.leethublog.domain.Difficulty;
import com.leethublog.domain.SolveStatus;
import lombok.Data;

@Data
public class SolveRequestDto {
    // Problem Info
    private String problemTitle;
    private String problemUrl;
    private String problemDescription; // Add field for the problem description
    private Difficulty problemDifficulty;
    private SolveStatus solveStatus; // Changed from perceivedDifficulty
    private String code;
    private String language;

    // Performance Metrics
    private Double runtimePercentile;
    private Integer runtimeMs;
    private Double memoryPercentile;
    private Double memoryMb;
}
