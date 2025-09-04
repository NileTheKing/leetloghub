package com.leethublog.controller.api;

import com.leethublog.controller.dto.CreateDbRequestDto;
import com.leethublog.controller.dto.NotionPageDto;
import com.leethublog.service.NotionService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notion")
@RequiredArgsConstructor
public class NotionAPIController {

    private final NotionService notionService;

    @PostMapping("/databases")
    public ResponseEntity<CreateDbRequestDto> createNotionDatabase(@RequestBody CreateDbRequestDto createDbRequestDto, Authentication authentication) {
        String githubLogin = authentication.getName();
        notionService.createDatabase(createDbRequestDto.getPageId(), authentication);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/problems")
    public CreateDbRequestDto submitProblemsToNotion(@RequestBody List<NotionPageDto> notionPageDtoList, Authentication authentication) {
        String githubLogin = authentication.getName();
        notionService.submitProblemsToNotion(githubLogin, notionPageDtoList);
        return null;
    }


}