package com.leethublog.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "problem_statuses")
@Getter
@Setter
@NoArgsConstructor
public class ProblemStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private String problemUrl; // The unique identifier for the problem

    @Column(nullable = false)
    private Integer currentInterval = 0; // 0 for new problems, then 1, 7, 28 etc.

    @Column
    private LocalDate nextReviewDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus reviewStatus = ReviewStatus.REVIEWING;

    // Custom constructor for the "create" part of "find-or-create"
    public ProblemStatus(Member member, String problemUrl) {
        this.member = member;
        this.problemUrl = problemUrl;
    }
}
