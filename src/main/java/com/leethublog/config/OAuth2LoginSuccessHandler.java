package com.leethublog.config;

import com.leethublog.domain.Member;
import com.leethublog.repository.MemberRepository;
import com.leethublog.service.MemberService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberService memberService;
    private final MemberRepository memberRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Member member = memberRepository.findByGithubId(Long.parseLong(oAuth2User.getName()))
                .orElseThrow(() -> new RuntimeException("Member not found after OAuth2 login"));

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        String jwt = jwtTokenProvider.generateToken(member.getGithubId(), authorities);

        log.info("User '{}' has logged in successfully. Issuing JWT.", member.getGithubLogin());

        String targetUrl = UriComponentsBuilder.fromUriString("/auth-success.html")
                .queryParam("token", jwt)
                .queryParam("user", member.getGithubLogin())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
