package com.leethublog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login.html", "/popup.html", "/popup.js", "/popup.css", "/images/**").permitAll() // 정적 리소스, 로그인, 팝업 페이지 허용
                        .requestMatchers("/auth/github/login", "/login/oauth2/code/github").permitAll() // GitHub 로그인 과정 허용
                        .anyRequest().authenticated() // 나머지 모든 요청은 인증 필요
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/auth-success.html", true) // 로그인 성공 시 리디렉션
                        .failureUrl("/login-failure.html") // 로그인 실패 시 리디렉션
                );
        return http.build();
    }
}
