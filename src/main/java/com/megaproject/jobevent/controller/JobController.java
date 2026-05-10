package com.megaproject.jobevent.controller;

import com.megaproject.common.dto.PageDTO;
import com.megaproject.jobevent.dto.request.JobRequest;
import com.megaproject.jobevent.dto.response.JobResponse;
import com.megaproject.jobevent.service.JobService;
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
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ALUMNI')")
    public ResponseEntity<JobResponse> create(
            @Valid @RequestBody JobRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.create(req, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(jobService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> getAll() {
        return ResponseEntity.ok(jobService.getAllActive());
    }

    /** Paginated version for frontend infinite scroll */
    @GetMapping("/paged")
    public ResponseEntity<PageDTO<JobResponse>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PageDTO.from(jobService.getAllActivePaged(pageable)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','ALUMNI')")
    public ResponseEntity<List<JobResponse>> getMyJobs(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(jobService.getByCreator(jwt.getSubject()));
    }

    @GetMapping("/company/{name}")
    public ResponseEntity<List<JobResponse>> getByCompany(@PathVariable String name) {
        return ResponseEntity.ok(jobService.getByCompany(name));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ALUMNI')")
    public ResponseEntity<JobResponse> update(
            @PathVariable String id,
            @Valid @RequestBody JobRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(jobService.update(id, req, jwt.getSubject(),
                jwt.getClaimAsString("role")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ALUMNI')")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        jobService.softDelete(id, jwt.getSubject(), jwt.getClaimAsString("role"));
        return ResponseEntity.ok(Map.of("success", true, "message", "Job deactivated"));
    }
}
