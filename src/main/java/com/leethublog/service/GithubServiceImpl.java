package com.leethublog.service;

import com.leethublog.controller.dto.CreateRepoRequestDto;
import com.leethublog.controller.dto.GithubRepoDto;
import com.leethublog.controller.dto.SolveRequestDto;
import com.leethublog.domain.Member;
import com.leethublog.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GithubServiceImpl implements GithubService {

    private final MemberRepository memberRepository;
    private final EncryptionService encryptionService;
    private final WebClient.Builder webClientBuilder;

    private WebClient buildGithubWebClient(String accessToken) {
        return webClientBuilder.baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken) // Reverted to Bearer as per successful Postman test
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .defaultHeader(HttpHeaders.USER_AGENT, "LeetLogHub-App") // Use a custom user agent
                .build();
    }

    @Override
    public List<GithubRepoDto> getUserRepos(Authentication authentication) {
        Long githubId = Long.parseLong(authentication.getName());
        Member member = memberRepository.findByGithubId(githubId)
                .orElseThrow(() -> new IllegalStateException("Member not found"));

        String accessToken = encryptionService.decrypt(member.getEncryptedGithubToken());
        WebClient webClient = buildGithubWebClient(accessToken);

        return webClient.get()
                .uri("/user/repos?sort=updated&per_page=100")
                .retrieve()
                .bodyToFlux(GithubRepoDto.class)
                .collectList()
                .block();
    }

    @Override
    @Transactional
    public void linkRepository(String repoFullName, Authentication authentication) {
        Long githubId = Long.parseLong(authentication.getName());
        Member member = memberRepository.findByGithubId(githubId)
                .orElseThrow(() -> new IllegalStateException("Member not found"));

        member.setTargetRepo(repoFullName);
        memberRepository.save(member);
        log.info("User {} linked repository: {}", member.getGithubLogin(), repoFullName);
    }

    @Override
    @Transactional
    public GithubRepoDto createAndLinkRepository(CreateRepoRequestDto request, Authentication authentication) {
        Long githubId = Long.parseLong(authentication.getName());
        Member member = memberRepository.findByGithubId(githubId)
                .orElseThrow(() -> new IllegalStateException("Member not found"));

        String accessToken = encryptionService.decrypt(member.getEncryptedGithubToken());
        WebClient webClient = buildGithubWebClient(accessToken);

        GithubRepoDto newRepo = webClient.post()
                .uri("/user/repos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GithubRepoDto.class)
                .block();

        if (newRepo == null) {
            throw new RuntimeException("Failed to create GitHub repository. Response was null.");
        }

        log.info("Successfully created GitHub repository: {}", newRepo.getFullName());

        this.linkRepository(newRepo.getFullName(), authentication);

        return newRepo;
    }

    @Override
    @Transactional
    public void uploadSolutionFiles(Authentication authentication, SolveRequestDto solveInfo) {
        Long githubId = Long.parseLong(authentication.getName());
        Member member = memberRepository.findByGithubId(githubId)
                .orElseThrow(() -> new IllegalStateException("Member not found"));

        if (member.getTargetRepo() == null || member.getTargetRepo().isEmpty()) {
            log.warn("User {} has no target repository linked. Skipping file uploads.", member.getGithubLogin());
            return;
        }

        String accessToken = encryptionService.decrypt(member.getEncryptedGithubToken());
        String repoFullName = member.getTargetRepo();
        String folderName = solveInfo.getProblemTitle().replaceAll("[^a-zA-Z0-9-]", " ").trim().replace(" ", "-");

        // Step 1: Create README.md first. This will also create the directory.
        String readmeContent = buildMarkdownContent(solveInfo);
        boolean readmeSuccess = uploadFileToGithub(accessToken, repoFullName, folderName + "/README.md", "docs: " + solveInfo.getProblemTitle(), readmeContent, member.getGithubLogin());

        // Step 2: If README creation was successful, create the source code file.
        if (readmeSuccess) {
            String fileExtension = getFileExtensionForLanguage(solveInfo.getLanguage());
            String codeFileName = solveInfo.getProblemTitle().replaceAll("[^a-zA-Z0-9.-]", "_") + fileExtension;
            uploadFileToGithub(accessToken, repoFullName, folderName + "/" + codeFileName, "feat: solve " + solveInfo.getProblemTitle(), solveInfo.getCode(), member.getGithubLogin());
        }
    }

    private boolean uploadFileToGithub(String accessToken, String repoFullName, String filePath, String commitMessage, String content, String committerName) {
        String encodedContent = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        WebClient webClient = buildGithubWebClient(accessToken);

        Map<String, Object> committer = new HashMap<>();
        committer.put("name", committerName);
        committer.put("email", committerName + "@users.noreply.github.com");

        Map<String, Object> body = new HashMap<>();
        body.put("message", commitMessage);
        body.put("content", encodedContent);
        body.put("committer", committer);

        log.info("Attempting to upload to GitHub: repo={}, path={}, message={}", repoFullName, filePath, commitMessage);

        String[] repoParts = repoFullName.split("/");
        if (repoParts.length != 2) {
            log.error("Invalid repoFullName format: {}", repoFullName);
            return false;
        }
        String owner = repoParts[0];
        String repo = repoParts[1];

        try {
            webClient.put()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, filePath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.info("Successfully uploaded {} to repository {}", filePath, repoFullName);
            return true;

        } catch (WebClientResponseException e) {
            log.error("Failed to upload {} to GitHub. Status: {}, Body: {}", filePath, e.getRawStatusCode(), e.getResponseBodyAsString());
            return false;
        }
    }

    private String buildMarkdownContent(SolveRequestDto solveInfo) {
        return new StringBuilder()
                .append("# ").append(solveInfo.getProblemTitle()).append("\n\n")
                .append("## Description\n\n")
                .append(solveInfo.getProblemDescription()).append("\n\n")
                .append("## Performance\n\n")
                .append("- **Runtime**: ").append(solveInfo.getRuntimeMs()).append(" ms (beats ").append(solveInfo.getRuntimePercentile()).append("%)\n")
                .append("- **Memory**: ").append(solveInfo.getMemoryMb()).append(" MB (beats ").append(solveInfo.getMemoryPercentile()).append("%)\n")
                .toString();
    }

    private String getFileExtensionForLanguage(String language) {
        if (language == null) return ".txt";
        switch (language.toLowerCase()) {
            case "java": return ".java";
            case "python":
            case "python3": return ".py";
            case "c++": return ".cpp";
            case "c": return ".c";
            case "c#": return ".cs";
            case "javascript": return ".js";
            case "typescript": return ".ts";
            case "go": return ".go";
            case "kotlin": return ".kt";
            case "rust": return ".rs";
            case "swift": return ".swift";
            case "scala": return ".scala";
            case "ruby": return ".rb";
            case "php": return ".php";
            case "mysql":
            case "mssql":
            case "oraclesql": return ".sql";
            default: return ".txt";
        }
    }
}
