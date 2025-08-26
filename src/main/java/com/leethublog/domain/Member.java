package com.leethublog.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "members")
@Getter
@Setter
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long githubId; // To store the immutable numeric ID from GitHub

    @Column(nullable = false)
    private String githubLogin; // To store the GitHub login name, which can change

    @Column(length = 1024)
    private String encryptedGithubToken;

    @Column(length = 1024)
    private String encryptedNotionToken;

    @Column
    private String targetRepo;

    @Column
    private String targetDbId;

}
