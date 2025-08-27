package com.leethublog.service;

import com.leethublog.controller.dto.NotionPageDto;
import com.leethublog.controller.dto.NotionTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotionServiceImpl implements NotionService {

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Override
    public void createPageInDatabase(String token, String databaseId, String title) {
        // This part is not in the scope of the current task.
        log.info("Creating Notion page with title: {}", title);
    }

    @Override
    public NotionTokenResponse requestAccessToken(String code) {
        log.info("Requesting Notion access token with Spring Security components.");

        ClientRegistration notionRegistration = this.clientRegistrationRepository.findByRegistrationId("notion");

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId(notionRegistration.getClientId())
                .authorizationUri(notionRegistration.getProviderDetails().getAuthorizationUri())
                .redirectUri(notionRegistration.getRedirectUri())
                .scopes(notionRegistration.getScopes())
                .state("state-dummy") // A state parameter is required by the spec
                .build();

        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success(code)
                .redirectUri(notionRegistration.getRedirectUri())
                .build();

        OAuth2AuthorizationExchange authorizationExchange = new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse);

        OAuth2AuthorizationCodeGrantRequest grantRequest = new OAuth2AuthorizationCodeGrantRequest(notionRegistration, authorizationExchange);

        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
        OAuth2AccessTokenResponse tokenResponse = tokenResponseClient.getTokenResponse(grantRequest);

        if (tokenResponse == null) {
            throw new RuntimeException("Failed to retrieve Notion access token: response was null.");
        }

        log.info("Successfully retrieved Notion access token.");

        // Extract additional parameters and build NotionTokenResponse
        Map<String, Object> additionalParams = tokenResponse.getAdditionalParameters();
        NotionTokenResponse notionTokenResponse = new NotionTokenResponse();
        notionTokenResponse.setAccessToken(tokenResponse.getAccessToken().getTokenValue());
        notionTokenResponse.setWorkspaceName((String) additionalParams.get("workspace_name"));
        notionTokenResponse.setWorkspaceId((String) additionalParams.get("workspace_id"));
        notionTokenResponse.setBotId((String) additionalParams.get("bot_id"));

        return notionTokenResponse;
    }

    @Override
    public List<NotionPageDto> getAvailablePages(Authentication authentication) {
        // TODO: Implement this method. It should:
        // 1. Get the Member entity based on the Authentication principal.
        // 2. Decrypt the Notion access token.
        // 3. Use the token to call Notion's /v1/search endpoint to find pages shared with the integration.
        // 4. Map the results to a List<NotionPageDto> and return it.
        log.warn("getAvailablePages is not implemented. Returning mock data.");
        NotionPageDto page1 = new NotionPageDto();
        page1.setId("mock-page-id-1");
        page1.setTitle("My Private Journal 📓");
        return Collections.singletonList(page1);
    }

    @Override
    public void createDatabase(String pageId, Authentication authentication) {
        // TODO: Implement this method. It should:
        // 1. Get the Member entity based on the Authentication principal.
        // 2. Decrypt the Notion access token.
        // 3. Use the token to call Notion's /v1/databases endpoint to create a new database with the given pageId as the parent.
        // 4. Save the new database ID to the Member entity.
        log.warn("createDatabase is not implemented. Mocking action.");
    }
}

