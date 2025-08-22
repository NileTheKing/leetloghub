package com.leetloghub.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String githubUsername;

    @Column(length = 1024)
    private String encryptedGithubToken;

    @Column(length = 1024)
    private String encryptedNotionToken;

    @Column
    private String targetRepo;

    @Column
    private String targetDbId;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    public String getEncryptedGithubToken() {
        return encryptedGithubToken;
    }

    public void setEncryptedGithubToken(String encryptedGithubToken) {
        this.encryptedGithubToken = encryptedGithubToken;
    }

    public String getEncryptedNotionToken() {
        return encryptedNotionToken;
    }

    public void setEncryptedNotionToken(String encryptedNotionToken) {
        this.encryptedNotionToken = encryptedNotionToken;
    }

    public String getTargetRepo() {
        return targetRepo;
    }

    public void setTargetRepo(String targetRepo) {
        this.targetRepo = targetRepo;
    }

    public String getTargetDbId() {
        return targetDbId;
    }

    public void setTargetDbId(String targetDbId) {
        this.targetDbId = targetDbId;
    }
}
