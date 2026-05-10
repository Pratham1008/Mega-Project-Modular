package com.megaproject.profile.service;

import com.megaproject.auth.service.AuthService;
import com.megaproject.auth.model.Role;
import com.megaproject.profile.dto.request.*;
import com.megaproject.profile.dto.response.*;
import com.megaproject.profile.exception.*;
import com.megaproject.profile.mapper.ProfileMapper;
import com.megaproject.profile.model.*;
import com.megaproject.profile.repository.ProfileRepository;
import com.megaproject.auth.repository.UserRepository;
import com.megaproject.auth.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public EducationalProfileResponse createEducationalProfile(EducationalProfileRequest req) {
        if (profileRepository.existsByUserId(req.getUserId()))
            throw new ProfileAlreadyExistsException("Profile already exists for userId: " + req.getUserId());
        if (profileRepository.existsByRegistrationNumber(req.getRegistrationNumber()))
            throw new ProfileAlreadyExistsException("Registration number already in use: " + req.getRegistrationNumber());

        ProfileDocument doc = profileMapper.toDocument(req);
        doc.setProfileType(determineType(req.getPassingYear()));
        ProfileDocument saved = profileRepository.save(doc);

        if (saved.getProfileType() == ProfileType.ALUMNI) {
            authService.updateUserRole(saved.getUserId(), Role.ALUMNI);
        } else {
            authService.updateUserRole(saved.getUserId(), Role.STUDENT);
        }

        return profileMapper.toEducationalResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.subject")
    public EducationalProfileResponse updateEducationalProfile(String userId, EducationalProfileRequest req) {
        ProfileDocument doc = getDocumentByUserId(userId);
        profileMapper.updateDocumentFromRequest(req, doc);
        doc.setProfileType(determineType(req.getPassingYear()));
        ProfileDocument saved = profileRepository.save(doc);

        if (saved.getProfileType() == ProfileType.ALUMNI) {
            authService.updateUserRole(userId, Role.ALUMNI);
        } else {
            authService.updateUserRole(userId, Role.STUDENT);
        }

        return profileMapper.toEducationalResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public FacultyProfileResponse createFacultyProfile(FacultyProfileRequest req) {
        if (profileRepository.existsByEmail(req.getEmail()))
            throw new ProfileAlreadyExistsException("Profile already exists for email: " + req.getEmail());

        String email = req.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            String prefix = email.split("@")[0];
            String defaultPassword = "KIT@" + prefix;
            User newUser = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(defaultPassword))
                    .role(Role.FACULTY)
                    .verified(true)
                    .build();
            return userRepository.save(newUser);
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

    public EducationalProfileResponse getEducationalProfile(String userId) {
        return profileMapper.toEducationalResponse(getDocumentByUserId(userId));
    }

    public FacultyProfileResponse getFacultyProfile(String userId) {
        return profileMapper.toFacultyResponse(getDocumentByUserId(userId));
    }

    public List<ProfileSummaryResponse> getProfilesByType(ProfileType type) {
        return profileRepository.findByProfileTypeAndDeletedFalse(type)
                .stream()
                .map(profileMapper::toSummary)
                .toList();
    }

    public List<ProfileSummaryResponse> getAllProfiles() {
        return profileRepository.findByDeletedFalse()
                .stream()
                .map(profileMapper::toSummary)
                .toList();
    }

    // ── Paginated versions (for frontend infinite scroll) ─────────────────────
    public Page<ProfileSummaryResponse> getProfilesByTypePaged(ProfileType type, Pageable pageable) {
        return profileRepository.findByProfileTypeAndDeletedFalseAndApprovedTrue(type, pageable)
                .map(profileMapper::toSummary);
    }

    public Page<ProfileSummaryResponse> getAllProfilesPaged(Pageable pageable) {
        return profileRepository.findByDeletedFalse(pageable)
                .map(profileMapper::toSummary);
    }

    public List<ProfileSummaryResponse> getBatchMates(String department, int passingYear) {
        return profileRepository.findByDepartmentAndPassingYearAndDeletedFalse(department, passingYear)
                .stream()
                .filter(ProfileDocument::isApproved)
                .map(profileMapper::toSummary)
                .toList();
    }

    public Map<String, Long> getProfileCounts() {
        long alumni = profileRepository.countByProfileTypeAndDeletedFalseAndApprovedTrue(ProfileType.ALUMNI);
        long student = profileRepository.countByProfileTypeAndDeletedFalseAndApprovedTrue(ProfileType.STUDENT);
        long faculty = profileRepository.countByProfileTypeAndDeletedFalseAndApprovedTrue(ProfileType.FACULTY);
        return Map.of(
                "alumni", alumni,
                "student", student,
                "faculty", faculty,
                "total", alumni + student + faculty
        );
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.subject")
    public void deleteProfile(String userId) {
        ProfileDocument doc = getDocumentByUserId(userId);
        doc.setDeleted(true);
        profileRepository.save(doc);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public EducationalProfileResponse changeProfileType(String userId, ProfileType newType) {
        ProfileDocument doc = getDocumentByUserId(userId);
        doc.setProfileType(newType);
        ProfileDocument saved = profileRepository.save(doc);

        if (newType == ProfileType.ALUMNI) {
            authService.updateUserRole(userId, Role.ALUMNI);
        } else if (newType == ProfileType.STUDENT) {
            authService.updateUserRole(userId, Role.STUDENT);
        } else if (newType == ProfileType.FACULTY) {
            authService.updateUserRole(userId, Role.FACULTY);
        }

        return profileMapper.toEducationalResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public EducationalProfileResponse approveProfile(String userId) {
        ProfileDocument doc = getDocumentByUserId(userId);
        doc.setApproved(true);
        return profileMapper.toEducationalResponse(profileRepository.save(doc));
    }

    private ProfileDocument getDocumentByUserId(String userId) {
        return profileRepository.findByUserId(userId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found for userId: " + userId));
    }

    private ProfileType determineType(int passingYear) {
        int currentYear = java.time.Year.now().getValue();
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        // If passing year is in the past, or if passing year is current year and we are past August, they are ALUMNI.
        if (passingYear < currentYear || (passingYear == currentYear && currentMonth >= 8)) {
            return ProfileType.ALUMNI;
        }
        return ProfileType.STUDENT;
    }
}
