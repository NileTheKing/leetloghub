package com.leethublog.service;

import com.leethublog.controller.dto.CreateDbRequestDto;
import com.leethublog.controller.dto.NotionPageDto;
import com.leethublog.controller.dto.NotionTokenResponse;
import com.leethublog.domain.Member;
import com.leethublog.repository.MemberRepository;
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
import org.springframework.web.reactive.function.client.WebClient;


import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotionServiceImpl implements NotionService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final MemberRepository memberRepository;
    private final EncryptionService encryptionService;
    //private final WebClient webClient;

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
        if (tokenResponse.getRefreshToken() != null) {
            notionTokenResponse.setRefreshToken(tokenResponse.getRefreshToken().getTokenValue());
        }
        notionTokenResponse.setWorkspaceName((String) additionalParams.get("workspace_name"));
        notionTokenResponse.setWorkspaceId((String) additionalParams.get("workspace_id"));
        notionTokenResponse.setBotId((String) additionalParams.get("bot_id"));
        notionTokenResponse.setDuplicatedTemplateId((String) additionalParams.get("duplicated_template_id"));

        return notionTokenResponse;
    }

    @Override
    public void submitProblemsToNotion(String githubLogin, Iterable<NotionPageDto> notionPageDtoList) {

    }

    @Override
    public List<NotionPageDto> getAvailablePages(Authentication authentication) {
        return List.of();
    }

    @Override
    public CreateDbRequestDto createDatabase(String pageId, Authentication authentication) {
        String userGithubId = authentication.getName();
        Member member = memberRepository.findByGithubId(Long.parseLong(userGithubId))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userGithubId));
        String decryptedNotionAccessToken = encryptionService.decrypt(Objects.requireNonNull(member.getEncryptedNotionToken()));



    }
}

