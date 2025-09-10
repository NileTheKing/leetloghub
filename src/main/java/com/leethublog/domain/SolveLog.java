package com.leethublog.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "solve_logs")
@Getter
@Setter
public class SolveLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String problemTitle;

    @Column(nullable = false)
    private String problemUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty problemDifficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SolveStatus solveStatus;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime solvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
