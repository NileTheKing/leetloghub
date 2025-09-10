package com.leethublog.controller;

import com.leethublog.controller.dto.SolveRequestDto;
import com.leethublog.service.SolveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solves")
@RequiredArgsConstructor
@Slf4j
public class SolveController {

    private final SolveService solveService;

    @PostMapping
    public ResponseEntity<Void> logSolution(@RequestBody SolveRequestDto solveRequest, Authentication authentication) {
        log.info("Received solve request: {}", solveRequest);
        solveService.processNewSolve(authentication, solveRequest);
        return ResponseEntity.ok().build();
    }
}
