package com.leethublog.service;

import com.leethublog.controller.dto.NotionPageDto;
import com.leethublog.controller.dto.NotionTokenResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface NotionService {

    NotionTokenResponse requestAccessToken(String code);

    List<NotionPageDto> getAvailablePages(Authentication authentication);

    void createDatabase(String pageId, Authentication authentication);

    void submitProblemsToNotion(String githubLogin, Iterable<NotionPageDto> notionPageDtoList);

}