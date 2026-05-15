package com.megaproject.stories.controller;

import com.megaproject.stories.dto.SuccessStoryRequest;
import com.megaproject.stories.dto.SuccessStoryResponse;
import com.megaproject.stories.service.SuccessStoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stories")
@RequiredArgsConstructor
public class SuccessStoryController {

    private final SuccessStoryService storyService;

    
    @GetMapping
    public ResponseEntity<List<SuccessStoryResponse>> getAll() {
        return ResponseEntity.ok(storyService.getAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessStoryResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(storyService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<SuccessStoryResponse> create(
            @Valid @RequestBody SuccessStoryRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storyService.create(req, jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        boolean isAdmin = "ADMIN".equals(jwt.getClaimAsString("role"));
        storyService.softDelete(id, jwt.getSubject(), isAdmin);
        return ResponseEntity.ok(Map.of("success", true, "message", "Story deactivated"));
    }
}
