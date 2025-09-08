package com.leethublog.service;

import com.leethublog.controller.dto.CreateRepoRequestDto;
import com.leethublog.controller.dto.GithubRepoDto;
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

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GithubServiceImpl implements GithubService {

    private final MemberRepository memberRepository;
    private final EncryptionService encryptionService;
    private final WebClient.Builder webClientBuilder;

    @Override
    public List<GithubRepoDto> getUserRepos(Authentication authentication) {
        Long githubId = Long.parseLong(authentication.getName());
        Member member = memberRepository.findByGithubId(githubId)
                .orElseThrow(() -> new IllegalStateException("Member not found"));

        String accessToken = encryptionService.decrypt(member.getEncryptedGithubToken());

        WebClient webClient = webClientBuilder.baseUrl("https://api.github.com").build();

        List<GithubRepoDto> allRepos = webClient.get()
                .uri("/user/repos?sort=updated&per_page=100")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToFlux(GithubRepoDto.class)
                .collectList()
                .block();

        if (allRepos == null) {
            return List.of();
        }

        // Filter to only include repos where the user has push access
        return allRepos.stream()
                .filter(repo -> repo.getPermissions() != null && repo.getPermissions().isPush())
                .toList();
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

        WebClient webClient = webClientBuilder.baseUrl("https://api.github.com").build();

        // Call GitHub API to create a new repository
        GithubRepoDto newRepo = webClient.post()
                .uri("/user/repos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GithubRepoDto.class)
                .block();

        if (newRepo == null) {
            throw new RuntimeException("Failed to create GitHub repository. Response was null.");
        }

        log.info("Successfully created GitHub repository: {}", newRepo.getFullName());

        // After creating, call the linkRepository method to set it as the target.
        this.linkRepository(newRepo.getFullName(), authentication);

        return newRepo;
    }
}