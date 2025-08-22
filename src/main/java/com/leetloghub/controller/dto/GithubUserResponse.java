package com.leetloghub.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GithubUserResponse {

    @JsonProperty("login")
    private String login;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }
}
