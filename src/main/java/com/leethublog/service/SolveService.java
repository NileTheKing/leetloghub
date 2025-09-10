package com.leethublog.service;

import com.leethublog.controller.dto.SolveRequestDto;
import org.springframework.security.core.Authentication;

public interface SolveService {
    void processNewSolve(Authentication authentication, SolveRequestDto solveRequest);

}
