package com.leethublog.controller.api;

import com.leethublog.controller.dto.CreateDbRequestDto;
import com.leethublog.controller.dto.NotionPageDto;
import com.leethublog.service.NotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    /**
     * Gets a list of pages the user has granted access to.
     */
    @GetMapping("/pages")
    public ResponseEntity<List<NotionPageDto>> getAvailablePages(Authentication authentication) {
        return ResponseEntity.ok(notionService.getAvailablePages(authentication));
    }

    /**
     * Creates the LeetLogHub database in the specified parent page.
     */
    @PostMapping("/database")
    public ResponseEntity<Void> createDatabase(@RequestBody CreateDbRequestDto request, Authentication authentication) {
        notionService.createDatabase(request.getPageId(), authentication);
        return ResponseEntity.ok().build();
    }
}