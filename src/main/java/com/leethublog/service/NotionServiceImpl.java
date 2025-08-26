package com.leethublog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public String requestAccessToken(String code) {
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
        return tokenResponse.getAccessToken().getTokenValue();
    }
}

