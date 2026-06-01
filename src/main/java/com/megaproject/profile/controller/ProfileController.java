package com.megaproject.profile.controller;

import com.megaproject.common.dto.PageDTO;
import com.megaproject.profile.dto.request.EducationalProfileRequest;
import com.megaproject.profile.dto.request.FacultyProfileRequest;
import com.megaproject.profile.dto.response.*;
import com.megaproject.profile.exception.UnauthorizedProfileAccessException;
import com.megaproject.profile.model.ProfileType;
import com.megaproject.profile.service.AlumniSearchService;
import com.megaproject.profile.service.ProfileCommandService;
import com.megaproject.profile.service.ProfileQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Thin HTTP adapter — routes requests to the correct service.
 * No repository injection, no business logic, no data mapping.
 */
@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileCommandService profileCommandService;
    private final ProfileQueryService profileQueryService;
    private final AlumniSearchService alumniSearchService;

    @PostMapping("/educational")
    public ResponseEntity<EducationalProfileResponse> createEducational(
            @Valid @RequestBody EducationalProfileRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        validateOwnership(jwt, req.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileCommandService.createEducationalProfile(req));
    }

    @PutMapping("/educational/{userId}")
    public ResponseEntity<EducationalProfileResponse> updateEducational(
            @PathVariable String userId,
            @Valid @RequestBody EducationalProfileRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        validateOwnershipOrAdmin(jwt, userId);
        return ResponseEntity.ok(profileCommandService.updateEducationalProfile(userId, req));
    }

    @GetMapping("/educational/{userId}")
    public ResponseEntity<EducationalProfileResponse> getEducational(@PathVariable String userId) {
        return ResponseEntity.ok(profileQueryService.getEducationalProfile(userId));
    }

    @PostMapping("/faculty")
    public ResponseEntity<FacultyProfileResponse> createFaculty(@Valid @RequestBody FacultyProfileRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileCommandService.createFacultyProfile(req));
    }

    @PutMapping("/faculty/{userId}")
    public ResponseEntity<FacultyProfileResponse> updateFaculty(
            @PathVariable String userId,
            @Valid @RequestBody FacultyProfileRequest req) {
        return ResponseEntity.ok(profileCommandService.updateFacultyProfile(userId, req));
    }

    @GetMapping("/faculty/{userId}")
    public ResponseEntity<FacultyProfileResponse> getFaculty(@PathVariable String userId) {
        return ResponseEntity.ok(profileQueryService.getFacultyProfile(userId));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCounts() {
        return ResponseEntity.ok(profileQueryService.getProfileCounts());
    }

    @GetMapping
    public ResponseEntity<List<ProfileSummaryResponse>> list(@RequestParam(required = false) ProfileType type) {
        if (type != null) return ResponseEntity.ok(profileQueryService.getProfilesByType(type));
        return ResponseEntity.ok(profileQueryService.getAllProfiles());
    }

    @GetMapping("/paged")
    public ResponseEntity<PageDTO<ProfileSummaryResponse>> listPaged(
            @RequestParam(required = false) ProfileType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        if (type != null) return ResponseEntity.ok(PageDTO.from(profileQueryService.getProfilesByTypePaged(type, pageable)));
        return ResponseEntity.ok(PageDTO.from(profileQueryService.getAllProfilesPaged(pageable)));
    }

    @GetMapping("/batch")
    public ResponseEntity<PageDTO<ProfileSummaryResponse>> getBatchMates(
            @RequestParam String department,
            @RequestParam int passingYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(PageDTO.from(profileQueryService.getBatchMatesPaged(department, passingYear, pageable)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {
        validateOwnershipOrAdmin(jwt, userId);
        profileCommandService.deleteProfile(userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Profile deleted"));
    }

    @PatchMapping("/{userId}/approve")
    public ResponseEntity<EducationalProfileResponse> approve(@PathVariable String userId) {
        return ResponseEntity.ok(profileCommandService.approveProfile(userId));
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EducationalProfileResponse> changeRole(
            @PathVariable String userId,
            @RequestParam ProfileType type) {
        return ResponseEntity.ok(profileCommandService.changeProfileType(userId, type));
    }

    @GetMapping("/map")
    public ResponseEntity<PageDTO<ProfileMapResponse>> mapProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 1000));
        return ResponseEntity.ok(PageDTO.from(profileQueryService.getMapProfiles(ProfileType.ALUMNI, pageable)));
    }

    @GetMapping("/search/alumni")
    public ResponseEntity<PageDTO<AlumniSearchResponse>> searchAlumni(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "company", required = false) String company,
            @RequestParam(value = "passingYear", required = false) Integer passingYear,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageDTO.from(
                alumniSearchService.searchWithFilters(query, department, company, passingYear, location, page, size)));
    }

    private void validateOwnership(Jwt jwt, String userId) {
        if (!jwt.getSubject().equals(userId)) {
            throw new UnauthorizedProfileAccessException("User ID mismatch: you cannot create a profile for another user");
        }
    }

    private void validateOwnershipOrAdmin(Jwt jwt, String userId) {
        boolean isAdmin = "ADMIN".equals(jwt.getClaimAsString("role"));
        if (!jwt.getSubject().equals(userId) && !isAdmin) {
            throw new UnauthorizedProfileAccessException("Access denied: you can only modify your own profile");
        }
    }
}
