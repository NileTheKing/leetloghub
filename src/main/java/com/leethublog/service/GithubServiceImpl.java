package com.leethublog.service;

import com.leethublog.controller.dto.GithubRepoDto;
import com.leethublog.domain.Member;
import com.leethublog.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GithubServiceImpl implements GithubService {

    private final MemberRepository memberRepository;
    private final EncryptionService encryptionService;
    // WebClient를 Bean으로 관리하는 것이 좋지만, 여기서는 편의상 직접 생성합니다.
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.github.com")
            .defaultHeader("Accept", "application/vnd.github+json")
            .build();

    @Override
    public void createRepo(String token, String repoName) {
        // TODO: Implement repository creation logic
    }

    @Override
    public List<GithubRepoDto> getUserRepos(Authentication authentication) {
        // 1. Authentication 객체에서 사용자의 고유 GitHub ID를 꺼냅니다.
        long userGithubId = Long.parseLong(authentication.getName());

        // 2. 그 ID를 사용해서 우리 DB에서 해당 회원을 찾습니다.
        Member member = memberRepository.findByGithubId(userGithubId)
                .orElseThrow(() -> new IllegalArgumentException("No user found with GitHub ID: " + userGithubId));

        // 3. 회원 정보에 저장된 암호화된 GitHub 액세스 토큰을 꺼내서 복호화합니다.
        String decryptedToken = encryptionService.decrypt(member.getEncryptedGithubToken());

        // 4. WebClient를 사용하여 GitHub API를 호출합니다.
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{username}/repos")
                        .queryParam("type", "all")
                        .queryParam("sort", "updated")
                        .build(member.getGithubLogin()))
                .headers(headers -> headers.setBearerAuth(decryptedToken))
                .retrieve() // 요청 실행
                .bodyToFlux(GithubRepoDto.class) // 응답을 GithubRepoDto의 Flux로 변환
                .collectList() // Flux를 List를 담은 Mono로 변환
                .block(); // 비동기 작업이 끝날 때까지 기다리고 최종 결과를 List로 꺼냄
    }
}
