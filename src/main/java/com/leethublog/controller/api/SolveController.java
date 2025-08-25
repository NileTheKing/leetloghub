package com.leethublog.controller.api;

import com.leethublog.controller.dto.SolveRequestDto;
import com.leethublog.controller.dto.UpdateDifficultyRequestDto;
import com.leethublog.service.SolveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/solves")
@RequiredArgsConstructor
public class SolveController {

    private final SolveService solveService;

//    public SolveController(SolveService solveService) {
//        this.solveService = solveService;
//    }

    /**
     * 문제 풀이 기록을 생성합니다.
     */
    @PostMapping
    public ResponseEntity<String> logSolution(@RequestBody SolveRequestDto requestDto) {
        // In a real application, username should be retrieved from the security context (e.g., JWT)
        solveService.logSolution(
                requestDto.getUsername(),
                requestDto.getProblemTitle(),
                requestDto.getCode(),
                requestDto.getDirectoryPath()
        );
        return ResponseEntity.ok("Solution logged successfully.");
    }

    /**
     * 기록된 문제의 체감 난이도를 수정합니다.
     */
    @PutMapping("/{solveId}/difficulty")
    public ResponseEntity<String> updateDifficulty(@PathVariable Long solveId, @RequestBody UpdateDifficultyRequestDto requestDto) {
        // This requires a new method in SolveService, e.g., updateDifficulty(solveId, difficulty)
        // TODO: Implement the service logic for updating difficulty
        System.out.println("Updating difficulty for solve " + solveId + " to " + requestDto.getDifficulty());
        return ResponseEntity.ok("Difficulty updated.");
    }
}
