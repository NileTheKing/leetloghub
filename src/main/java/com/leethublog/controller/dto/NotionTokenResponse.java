package com.leethublog.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotionTokenResponse {

    @JsonProperty("access_token")
    private String accessToken; //An access token used to authorize requests to the Notion API.
    @JsonProperty("refresh_token") //이거 db저장하나?
    private String refreshToken;//A refresh token used to generate a new access token
    @JsonProperty("bot_id")//An identifier for this authorization.
    private String botId;
    @JsonProperty /*An object containing information about who can view and share this integration.
     { "workspace": true } is returned for installations of workspace-level tokens. For user level tokens,
     a user object is returned.*/
    private Owner owner;
    @JsonProperty("workspace_id") //The ID of the workspace where this authorization took place.
    private String workspaceId;
    @JsonProperty("workspace_name")//A human-readable name that can be used to display this authorization in the UI.
    private String workspaceName;
    @JsonProperty("duplicated_template_id")
    private String duplicatedTemplateId; // When a user duplicates a template, this is the ID of the duplicated page.



}
