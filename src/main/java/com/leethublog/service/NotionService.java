package com.leethublog.service;

import com.leethublog.controller.dto.CreateDbRequestDto;
import com.leethublog.controller.dto.NotionPageDto;
import com.leethublog.controller.dto.NotionTokenResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface NotionService {
    NotionTokenResponse requestAccessToken(String code);
    void submitProblemsToNotion(String githubLogin, Iterable<NotionPageDto> notionPageDtoList);

    List<NotionPageDto> getAvailablePages(Authentication authentication);

    CreateDbRequestDto createDatabase(String pageId, Authentication authentication);
}
