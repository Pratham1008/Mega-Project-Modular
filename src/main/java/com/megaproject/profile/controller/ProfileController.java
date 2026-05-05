package com.megaproject.profile.controller;

import com.megaproject.profile.dto.request.*;
import com.megaproject.profile.dto.response.*;
import com.megaproject.profile.exception.UnauthorizedProfileAccessException;
import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.model.ProfileType;
import com.megaproject.profile.repository.ProfileRepository;
import com.megaproject.profile.service.AlumniSearchService;
import com.megaproject.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileRepository profileRepository;
    private final AlumniSearchService alumniSearchService;

    // ---- Educational (Student / Alumni) ----

    @PostMapping("/educational")
    public ResponseEntity<EducationalProfileResponse> createEducational(
            @Valid @RequestBody EducationalProfileRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        validateOwnership(jwt, req.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileService.createEducationalProfile(req));
    }

    @PutMapping("/educational/{userId}")
    public ResponseEntity<EducationalProfileResponse> updateEducational(
            @PathVariable String userId,
            @Valid @RequestBody EducationalProfileRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        validateOwnershipOrAdmin(jwt, userId);
        return ResponseEntity.ok(profileService.updateEducationalProfile(userId, req));
    }

    @GetMapping("/educational/{userId}")
    public ResponseEntity<EducationalProfileResponse> getEducational(@PathVariable String userId) {
        return ResponseEntity.ok(profileService.getEducationalProfile(userId));
    }

    // ---- Faculty ----

    @PostMapping("/faculty")
    public ResponseEntity<FacultyProfileResponse> createFaculty(
            @Valid @RequestBody FacultyProfileRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileService.createFacultyProfile(req));
    }

    @PutMapping("/faculty/{userId}")
    public ResponseEntity<FacultyProfileResponse> updateFaculty(
            @PathVariable String userId,
            @Valid @RequestBody FacultyProfileRequest req) {
        return ResponseEntity.ok(profileService.updateFacultyProfile(userId, req));
    }

    @GetMapping("/faculty/{userId}")
    public ResponseEntity<FacultyProfileResponse> getFaculty(@PathVariable String userId) {
        return ResponseEntity.ok(profileService.getFacultyProfile(userId));
    }

    // ---- Listing ----

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCounts() {
        return ResponseEntity.ok(profileService.getProfileCounts());
    }


    @GetMapping
    public ResponseEntity<List<ProfileSummaryResponse>> list(
            @RequestParam(required = false) ProfileType type) {
        if (type != null) return ResponseEntity.ok(profileService.getProfilesByType(type));
        return ResponseEntity.ok(profileService.getAllProfiles());
    }

    // ---- Delete & Approve ----

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {
        validateOwnershipOrAdmin(jwt, userId);
        profileService.deleteProfile(userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Profile deleted"));
    }

    @PatchMapping("/{userId}/approve")
    public ResponseEntity<EducationalProfileResponse> approve(@PathVariable String userId) {
        return ResponseEntity.ok(profileService.approveProfile(userId));
    }

    // ---- World Map ----

    /**
     * Lightweight endpoint returning profiles that have a non-null location.
     * Used by the frontend world map to display alumni pins.
     * Public — no auth required.
     */
    @GetMapping("/map")
    public ResponseEntity<List<Map<String, Object>>> mapProfiles() {
        List<Map<String, Object>> pins = profileRepository.findByDeletedFalse()
                .stream()
                .filter(p -> p.getLocation() != null && !p.getLocation().isBlank())
                .map(p -> Map.<String, Object>of(
                        "userId",     p.getUserId(),
                        "fullName",   p.getFullName() != null ? p.getFullName() : "",
                        "photoUrl",   p.getPhotoUrl() != null ? p.getPhotoUrl() : "",
                        "location",   p.getLocation(),
                        "department", p.getDepartment() != null ? p.getDepartment() : "",
                        "profileType",p.getProfileType() != null ? p.getProfileType().name() : "",
                        "company",    p.getCompany() != null ? p.getCompany() : "",
                        "passingYear",p.getPassingYear() != null ? p.getPassingYear() : 0
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(pins);
    }

    // ---- Alumni Search (MongoDB Text Search) ----

    @GetMapping("/search/alumni")
    public ResponseEntity<List<AlumniSearchResponse>> searchAlumni(@RequestParam("q") String query) {
        return ResponseEntity.ok(alumniSearchService.search(query));
    }

    // ---- Helpers ----

    private void validateOwnership(Jwt jwt, String userId) {
        String subject = jwt.getSubject();
        if (!subject.equals(userId)) {
            throw new UnauthorizedProfileAccessException("User ID mismatch: you cannot create a profile for another user");
        }
    }

    private void validateOwnershipOrAdmin(Jwt jwt, String userId) {
        String subject = jwt.getSubject();
        boolean isAdmin = jwt.getClaimAsString("role") != null
                && jwt.getClaimAsString("role").equals("ADMIN");
        if (!subject.equals(userId) && !isAdmin) {
            throw new UnauthorizedProfileAccessException("Access denied: you can only modify your own profile");
        }
    }
}
