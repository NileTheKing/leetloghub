package com.leethublog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberService memberService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        Long githubId = Long.parseLong(oauth2User.getName());
        String githubLogin = (String) attributes.get("login");
        String accessToken = userRequest.getAccessToken().getTokenValue();

        log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        log.info("Raw GitHub Access Token for testing: {}", accessToken);
        log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        log.info("User loaded via OAuth2. GitHub ID: {}, Login: {}. Saving auth info.", githubId, githubLogin);

        memberService.saveGithubAuth(githubId, githubLogin, accessToken);

        return oauth2User;
    }
}
