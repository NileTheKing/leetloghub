package com.leethublog.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GithubRepoDto {

    private long id;
    private String name;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("private")
    private boolean isPrivate;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("default_branch")
    private String defaultBranch;

    private PermissionsDto permissions;

    // GitHub API의 permissions 객체를 매핑하기 위한 중첩 클래스
    @Data
    public static class PermissionsDto {
        private boolean admin;
        private boolean push;
        private boolean pull;
    }
}
