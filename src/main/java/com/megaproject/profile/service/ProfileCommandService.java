package com.megaproject.profile.service;

import com.megaproject.auth.model.Role;
import com.megaproject.auth.repository.UserRepository;
import com.megaproject.auth.service.AuthService;
import com.megaproject.common.util.PasswordGeneratorUtil;
import com.megaproject.notification.service.EmailService;
import com.megaproject.profile.dto.request.EducationalProfileRequest;
import com.megaproject.profile.dto.request.FacultyProfileRequest;
import com.megaproject.profile.dto.response.EducationalProfileResponse;
import com.megaproject.profile.dto.response.FacultyProfileResponse;
import com.megaproject.profile.exception.ProfileAlreadyExistsException;
import com.megaproject.profile.exception.ProfileNotFoundException;
import com.megaproject.profile.mapper.ProfileMapper;
import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.model.ProfileType;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Handles all WRITE operations on profiles (Command side of CQRS-lite).
 * Separated from ProfileQueryService so each class has one reason to change.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileCommandService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordGeneratorUtil passwordGenerator;

    // ── Educational Profile ───────────────────────────────────────────────

    public EducationalProfileResponse createEducationalProfile(EducationalProfileRequest req) {
        if (profileRepository.existsByUserId(req.getUserId()))
            throw new ProfileAlreadyExistsException(
                    "Profile already exists for userId: " + req.getUserId());
        if (profileRepository.existsByRegistrationNumber(req.getRegistrationNumber()))
            throw new ProfileAlreadyExistsException(
                    "Registration number already in use: " + req.getRegistrationNumber());

        ProfileDocument doc = profileMapper.toDocument(req);
        doc.setProfileType(determineType(req.getPassingYear()));
        ProfileDocument saved = profileRepository.save(doc);

        Role role = saved.getProfileType() == ProfileType.ALUMNI ? Role.ALUMNI : Role.STUDENT;
        authService.updateUserRole(saved.getUserId(), role);

        return profileMapper.toEducationalResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.subject")
    public EducationalProfileResponse updateEducationalProfile(
            String userId, EducationalProfileRequest req) {

        ProfileDocument doc = getDocumentByUserId(userId);
        profileMapper.updateDocumentFromRequest(req, doc);
        doc.setProfileType(determineType(req.getPassingYear()));
        ProfileDocument saved = profileRepository.save(doc);

        Role role = saved.getProfileType() == ProfileType.ALUMNI ? Role.ALUMNI : Role.STUDENT;
        authService.updateUserRole(userId, role);

        return profileMapper.toEducationalResponse(saved);
    }

    // ── Faculty Profile ───────────────────────────────────────────────────

    @PreAuthorize("hasRole('ADMIN')")
    public FacultyProfileResponse createFacultyProfile(FacultyProfileRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (profileRepository.existsByEmail(email))
            throw new ProfileAlreadyExistsException(
                    "Profile already exists for email: " + email);

        var user = userRepository.findByEmail(email).orElseGet(() -> {
            String generatedPassword = passwordGenerator.generate();
            var newUser = com.megaproject.auth.model.User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(generatedPassword))
                    .role(Role.FACULTY)
                    .verified(true)
                    .build();
            var savedUser = userRepository.save(newUser);
            emailService.sendCredentialsEmail(email, req.getFullName(), generatedPassword);
            log.info("Provisioned new Faculty account: {} (credentials sent via email)", email);
            return savedUser;
        });

        ProfileDocument doc = profileMapper.toDocument(req);
        doc.setUserId(user.getId());
        doc.setEmail(email);
        doc.setProfileType(ProfileType.FACULTY);
        doc.setApproved(true);
        ProfileDocument saved = profileRepository.save(doc);

        if (user.getRole() != Role.FACULTY) {
            authService.updateUserRole(saved.getUserId(), Role.FACULTY);
        }
        return profileMapper.toFacultyResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public FacultyProfileResponse updateFacultyProfile(String userId, FacultyProfileRequest req) {
        ProfileDocument doc = getDocumentByUserId(userId);
        profileMapper.updateDocumentFromRequest(req, doc);
        return profileMapper.toFacultyResponse(profileRepository.save(doc));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.subject")
    public void deleteProfile(String userId) {
        ProfileDocument doc = getDocumentByUserId(userId);
        doc.setDeleted(true);
        profileRepository.save(doc);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public EducationalProfileResponse approveProfile(String userId) {
        ProfileDocument doc = getDocumentByUserId(userId);
        doc.setApproved(true);
        return profileMapper.toEducationalResponse(profileRepository.save(doc));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public EducationalProfileResponse changeProfileType(String userId, ProfileType newType) {
        ProfileDocument doc = getDocumentByUserId(userId);
        doc.setProfileType(newType);
        ProfileDocument saved = profileRepository.save(doc);

        Role role = switch (newType) {
            case ALUMNI -> Role.ALUMNI;
            case STUDENT -> Role.STUDENT;
            case FACULTY -> Role.FACULTY;
        };
        authService.updateUserRole(userId, role);
        return profileMapper.toEducationalResponse(saved);
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private ProfileDocument getDocumentByUserId(String userId) {
        return profileRepository.findByUserId(userId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Profile not found for userId: " + userId));
    }

    private ProfileType determineType(int passingYear) {
        LocalDate now = LocalDate.now();
        if (passingYear < now.getYear() || (passingYear == now.getYear() && now.getMonthValue() >= 8))
            return ProfileType.ALUMNI;
        return ProfileType.STUDENT;
    }
}
