package com.leethublog.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SolveStatus {
    // Enum constants with a numeric value to represent proficiency level.
    // Higher is better.
    PERFECT("Perfect", 5),
    GOOD("Good", 4),
    STRUGGLED("Struggled", 3),
    HINT("Used Hint", 2),
    SOLUTION("Viewed Solution", 1);

    private final String displayName;
    private final int proficiency;
}
