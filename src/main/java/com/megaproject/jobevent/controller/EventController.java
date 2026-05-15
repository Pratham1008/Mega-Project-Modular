package com.megaproject.jobevent.controller;

import com.megaproject.common.dto.PageDTO;
import com.megaproject.jobevent.dto.request.EventRequest;
import com.megaproject.jobevent.dto.response.EventResponse;
import com.megaproject.jobevent.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<EventResponse> create(
            @Valid @RequestBody EventRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.create(req, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(eventService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAll() {
        return ResponseEntity.ok(eventService.getAllActive());
    }

    
    @GetMapping("/paged")
    public ResponseEntity<PageDTO<EventResponse>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PageDTO.from(eventService.getAllActivePaged(pageable)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<List<EventResponse>> getMyEvents(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eventService.getByCreator(jwt.getSubject()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<EventResponse> update(
            @PathVariable String id,
            @Valid @RequestBody EventRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eventService.update(id, req, jwt.getSubject(),
                jwt.getClaimAsString("role")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String id) {
        eventService.softDelete(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Event deactivated"));
    }

    
    @PostMapping("/{id}/rsvp")
    public ResponseEntity<EventResponse> rsvp(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eventService.rsvp(id, jwt.getSubject(), jwt.getClaimAsString("role")));
    }

    
    @DeleteMapping("/{id}/rsvp")
    public ResponseEntity<EventResponse> unrsvp(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eventService.unrsvp(id, jwt.getSubject()));
    }
}
