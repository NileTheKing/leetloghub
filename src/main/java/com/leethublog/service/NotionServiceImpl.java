package com.leethublog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.leethublog.controller.dto.NotionPageDto;
import com.leethublog.controller.dto.NotionSyncDto;
import com.leethublog.controller.dto.NotionTokenResponse;
import com.leethublog.domain.Member;
import com.leethublog.domain.ReviewStatus;
import com.leethublog.domain.SolveStatus;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;

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

        try {
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
        } catch (WebClientResponseException e) {
            log.error("Notion API Error while fetching pages. Status: {}, Body: {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    @Override
    public void createDatabase(String pageId, Authentication authentication) {
        Member member = findMemberByAuth(authentication);
        String accessToken = encryptionService.decrypt(member.getEncryptedNotionToken());
        Map<String, Object> requestBody = buildCreateDatabaseRequestBody(pageId);

        try {
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
        } catch (WebClientResponseException e) {
            log.error("Notion API Error during DB creation. Status: {}, Body: {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    @Override
    public void syncSolveToNotion(Member member, NotionSyncDto syncData) {
        String accessToken = encryptionService.decrypt(member.getEncryptedNotionToken());
        String databaseId = member.getTargetDbId();

        if (databaseId == null) {
            log.error("Notion Database ID is not set for user: {}", member.getGithubLogin());
            return;
        }

        try {
            String existingPageId = findExistingPageIdByTitle(accessToken, databaseId, syncData.getProblemTitle());

            if (existingPageId != null) {
                updateNotionPage(accessToken, existingPageId, syncData);
            } else {
                createNotionPage(accessToken, databaseId, syncData);
            }
        } catch (WebClientResponseException e) {
            log.error("Notion API Error during sync. Status: {}, Body: {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    private String findExistingPageIdByTitle(String accessToken, String databaseId, String problemTitle) {
        String queryBody = String.format("{ \"filter\": { \"property\": \"ProblemTitle\", \"title\": { \"equals\": \"%s\" } } }", problemTitle);

        JsonNode response = webClient.post()
                .uri("/v1/databases/" + databaseId + "/query")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(queryBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response != null && response.has("results") && response.get("results").size() > 0) {
            return response.get("results").get(0).get("id").asText();
        }
        return null;
    }

    private void updateNotionPage(String accessToken, String pageId, NotionSyncDto syncData) {
        Map<String, Object> properties = buildNotionProperties(syncData);
        Map<String, Object> requestBody = Map.of("properties", properties);

        webClient.patch()
                .uri("/v1/pages/" + pageId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        log.info("Successfully updated Notion page {} for problem {}", pageId, syncData.getProblemTitle());
    }

    private void createNotionPage(String accessToken, String databaseId, NotionSyncDto syncData) {
        Map<String, Object> properties = buildNotionProperties(syncData);
        Map<String, Object> requestBody = Map.of(
                "parent", Map.of("database_id", databaseId),
                "properties", properties
        );

        webClient.post()
                .uri("/v1/pages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        log.info("Successfully created new Notion page for problem {}", syncData.getProblemTitle());
    }

    private Map<String, Object> buildNotionProperties(NotionSyncDto syncData) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("ProblemTitle", Map.of("title", List.of(Map.of("type", "text", "text", Map.of("content", syncData.getProblemTitle())))));
        properties.put("URL", Map.of("url", syncData.getProblemUrl()));
        properties.put("Last Solved", Map.of("date", Map.of("start", syncData.getLastSolved().toString())));
        if (syncData.getNextReview() != null) {
            properties.put("Next Review", Map.of("date", Map.of("start", syncData.getNextReview().toString())));
        }
        properties.put("Difficulty", Map.of("select", Map.of("name", syncData.getProblemDifficulty().name().substring(0,1) + syncData.getProblemDifficulty().name().substring(1).toLowerCase())));
        properties.put("Solve Status", Map.of("select", Map.of("name", syncData.getSolveStatus().getDisplayName())));
        properties.put("Status", Map.of("select", Map.of("name", syncData.getReviewStatus() == ReviewStatus.MASTERED ? "Mastered" : "Reviewing")));
        properties.put("Attempts", Map.of("number", syncData.getAttempts()));
        properties.put("History", Map.of("rich_text", List.of(Map.of("type", "text", "text", Map.of("content", syncData.getHistorySummary())))));
        return properties;
    }

    private Map<String, Object> buildCreateDatabaseRequestBody(String pageId) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("parent", Map.of("type", "page_id", "page_id", pageId));
        List<Map<String, Object>> titleList = new ArrayList<>();
        titleList.add(Map.of("type", "text", "text", Map.of("content", "LeetLogHub Database")));
        requestBody.put("title", titleList);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("ProblemTitle", Map.of("title", Map.of()));
        properties.put("URL", Map.of("url", Map.of())); // Add the missing URL property definition
        properties.put("Last Solved", Map.of("date", Map.of()));
        properties.put("Next Review", Map.of("date", Map.of()));
        properties.put("Attempts", Map.of("number", Map.of("format", "number")));
        List<Map<String, String>> difficultyOptions = List.of(
                Map.of("name", "Easy", "color", "green"),
                Map.of("name", "Medium", "color", "yellow"),
                Map.of("name", "Hard", "color", "red")
        );
        properties.put("Difficulty", Map.of("select", Map.of("options", difficultyOptions)));
        List<Map<String, Object>> solveStatusOptions = new ArrayList<>();
        for (SolveStatus status : SolveStatus.values()) {
            solveStatusOptions.add(Map.of("name", status.getDisplayName()));
        }
        properties.put("Solve Status", Map.of("select", Map.of("options", solveStatusOptions)));
        properties.put("History", Map.of("rich_text", Map.of()));
        properties.put("Status", Map.of("select", Map.of("options", List.of(
                Map.of("name", "Reviewing", "color", "blue"),
                Map.of("name", "Mastered", "color", "purple")
        ))));
        requestBody.put("properties", properties);

        return requestBody;
    }

    private Member findMemberByAuth(Authentication authentication) {
        Long githubId = Long.parseLong(authentication.getName());
        return memberRepository.findByGithubId(githubId)
                .orElseThrow(() -> new IllegalStateException("Member not found for githubId: " + githubId));
    }
}
