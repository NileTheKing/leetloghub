package com.leetloghub.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    private String githubUsername;

    @Column(columnDefinition = "TEXT")
    private String encryptedGithubToken;

    @Column(columnDefinition = "TEXT")
    private String encryptedNotionToken;

    private String targetRepo;

    private String targetDbId;

    public Member(String githubUsername, String encryptedGithubToken, String encryptedNotionToken, String targetRepo, String targetDbId) {
        this.githubUsername = githubUsername;
        this.encryptedGithubToken = encryptedGithubToken;
        this.encryptedNotionToken = encryptedNotionToken;
        this.targetRepo = targetRepo;
        this.targetDbId = targetDbId;
    }
}