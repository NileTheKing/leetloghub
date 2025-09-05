package com.leethublog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.leethublog.controller.dto.NotionPageDto;
import com.leethublog.controller.dto.NotionTokenResponse;
import com.leethublog.domain.Member;
import com.leethublog.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotionServiceImpl implements NotionService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final MemberRepository memberRepository;
    private final EncryptionService encryptionService;
    private final WebClient.Builder webClientBuilder;
    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder
                .baseUrl("https://api.notion.com")
                .defaultHeader("Notion-Version", "2022-06-28")
                .build();
    }

    @Override
    public NotionTokenResponse requestAccessToken(String code) {
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
        ClientRegistration notionRegistration = this.clientRegistrationRepository.findByRegistrationId("notion");
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId(notionRegistration.getClientId())
                .authorizationUri(notionRegistration.getProviderDetails().getAuthorizationUri())
                .redirectUri(notionRegistration.getRedirectUri())
                .scopes(notionRegistration.getScopes())
                .state("state-dummy")
                .build();
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success(code)
                .redirectUri(notionRegistration.getRedirectUri())
                .build();
        OAuth2AuthorizationCodeGrantRequest grantRequest = new OAuth2AuthorizationCodeGrantRequest(notionRegistration, new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse));
        OAuth2AccessTokenResponse tokenResponse = tokenResponseClient.getTokenResponse(grantRequest);

        if (tokenResponse == null) {
            throw new RuntimeException("Failed to retrieve Notion access token.");
        }

        log.info("Successfully retrieved Notion access token.");
        Map<String, Object> additionalParams = tokenResponse.getAdditionalParameters();
        NotionTokenResponse notionTokenResponse = new NotionTokenResponse();
        notionTokenResponse.setAccessToken(tokenResponse.getAccessToken().getTokenValue());
        if (tokenResponse.getRefreshToken() != null) {
            notionTokenResponse.setRefreshToken(tokenResponse.getRefreshToken().getTokenValue());
        }
        notionTokenResponse.setWorkspaceName((String) additionalParams.get("workspace_name"));
        return notionTokenResponse;
    }

    @Override
    public List<NotionPageDto> getAvailablePages(Authentication authentication) {
        Member member = findMemberByAuth(authentication);
        String accessToken = encryptionService.decrypt(member.getEncryptedNotionToken());
        String requestBody = "{\"filter\":{\"property\":\"object\",\"value\":\"page\"}}";

        JsonNode response = webClient.post()
                .uri("/v1/search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        List<NotionPageDto> pages = new ArrayList<>();
        if (response != null && response.has("results")) {
            for (JsonNode result : response.get("results")) {
                NotionPageDto page = new NotionPageDto();
                page.setId(result.get("id").asText());
                JsonNode titleNode = result.at("/properties/title/title/0/text/content");
                page.setTitle(titleNode.isMissingNode() ? "Untitled" : titleNode.asText());
                pages.add(page);
            }
        }
        return pages;
    }

    @Override
    public void createDatabase(String pageId, Authentication authentication) {
        Member member = findMemberByAuth(authentication);
        String accessToken = encryptionService.decrypt(member.getEncryptedNotionToken());
        Map<String, Object> requestBody = buildCreateDatabaseRequestBody(pageId);

        JsonNode response = webClient.post()
                .uri("/v1/databases")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response != null && response.has("id")) {
            String dbId = response.get("id").asText();
            member.setTargetDbId(dbId);
            memberRepository.save(member);
            log.info("Successfully created Notion SRS database with ID: {} for user: {}", dbId, member.getGithubLogin());
        } else {
            log.error("Failed to create Notion database for user: {}. Response: {}", member.getGithubLogin(), response);
            throw new RuntimeException("Failed to create Notion database.");
        }
    }

    @Override
    public void submitProblemsToNotion(String githubLogin, Iterable<NotionPageDto> notionPageDtoList) {
        // TODO: Implement this method later
    }

    private Map<String, Object> buildCreateDatabaseRequestBody(String pageId) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("parent", Map.of("type", "page_id", "page_id", pageId));
        List<Map<String, Object>> titleList = new ArrayList<>();
        titleList.add(Map.of("type", "text", "text", Map.of("content", "LeetLogHub Database")));
        requestBody.put("title", titleList);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("ProblemTitle", Map.of("title", Map.of()));
        properties.put("Last Solved", Map.of("date", Map.of()));
        properties.put("Next Review", Map.of("date", Map.of()));
        properties.put("Attempts", Map.of("number", Map.of("format", "number")));
        List<Map<String, String>> difficultyOptions = List.of(
                Map.of("name", "Easy", "color", "green"),
                Map.of("name", "Medium", "color", "yellow"),
                Map.of("name", "Hard", "color", "red")
        );
        properties.put("Difficulty", Map.of("select", Map.of("options", difficultyOptions)));
        properties.put("Perceived Difficulty", Map.of("select", Map.of("options", difficultyOptions)));
        properties.put("History", Map.of("rich_text", Map.of()));
        requestBody.put("properties", properties);

        return requestBody;
    }

    private Member findMemberByAuth(Authentication authentication) {
        Long githubId = Long.parseLong(authentication.getName());
        return memberRepository.findByGithubId(githubId)
                .orElseThrow(() -> new IllegalStateException("Member not found for githubId: " + githubId));
    }
}