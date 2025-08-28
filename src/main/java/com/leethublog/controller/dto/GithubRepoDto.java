package com.leethublog.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GithubRepoDto {

    // 화면에 보여줄 저장소 이름 (예: "leetloghub")
    private String name;

    // 저장소를 식별할 고유한 전체 이름 (예: "yangnail/leetloghub")
    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("private")
    private boolean isPrivate;
}