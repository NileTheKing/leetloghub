package com.leetloghub.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
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


}
