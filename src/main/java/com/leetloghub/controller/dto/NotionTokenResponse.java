package com.leetloghub.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NotionTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("workspace_name")
    private String workspaceName;

    @JsonProperty("bot_id")
    private String botId;

    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getBotId() {
        return botId;
    }

    public void setBotId(String botId) {
        this.botId = botId;
    }
}
