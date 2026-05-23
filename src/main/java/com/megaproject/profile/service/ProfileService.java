package com.megaproject.profile.service;

import com.megaproject.auth.model.Role;
import com.megaproject.auth.repository.UserRepository;
import com.megaproject.auth.service.AuthService;
import com.megaproject.profile.dto.request.*;
import com.megaproject.profile.dto.response.*;
import com.megaproject.profile.exception.*;
import com.megaproject.profile.mapper.ProfileMapper;
import com.megaproject.profile.model.*;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
    private final MongoTemplate mongoTemplate;

    public EducationalProfileResponse createEducationalProfile(EducationalProfileRequest req) {
        if (profileRepository.existsByUserId(req.getUserId()))
            throw new ProfileAlreadyExistsException("Profile already exists for userId: " + req.getUserId());
        if (profileRepository.existsByRegistrationNumber(req.getRegistrationNumber()))
            throw new ProfileAlreadyExistsException("Registration number already in use: " + req.getRegistrationNumber());

        ProfileDocument doc = profileMapper.toDocument(req);
        doc.setProfileType(determineType(req.getPassingYear()));
        ProfileDocument saved = profileRepository.save(doc);

        Role role = saved.getProfileType() == ProfileType.ALUMNI ? Role.ALUMNI : Role.STUDENT;
        authService.updateUserRole(saved.getUserId(), role);

        return profileMapper.toEducationalResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.subject")
    public EducationalProfileResponse updateEducationalProfile(String userId, EducationalProfileRequest req) {
        ProfileDocument doc = getDocumentByUserId(userId);
        profileMapper.updateDocumentFromRequest(req, doc);
        doc.setProfileType(determineType(req.getPassingYear()));
        ProfileDocument saved = profileRepository.save(doc);

        Role role = saved.getProfileType() == ProfileType.ALUMNI ? Role.ALUMNI : Role.STUDENT;
        authService.updateUserRole(userId, role);

        return profileMapper.toEducationalResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public FacultyProfileResponse createFacultyProfile(FacultyProfileRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (profileRepository.existsByEmail(email))
            throw new ProfileAlreadyExistsException("Profile already exists for email: " + email);

        var user = userRepository.findByEmail(email).orElseGet(() -> {
            String prefix = email.split("@")[0];
            var newUser = com.megaproject.auth.model.User.builder()
                    .email(email)
                    .password(passwordEncoder.encode("KIT@" + prefix))
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
                .stream().map(profileMapper::toSummary).toList();
    }

    public List<ProfileSummaryResponse> getAllProfiles() {
        return profileRepository.findByDeletedFalse()
                .stream().map(profileMapper::toSummary).toList();
    }

    public Page<ProfileSummaryResponse> getProfilesByTypePaged(ProfileType type, Pageable pageable) {
        return profileRepository.findByProfileTypeAndDeletedFalseAndApprovedTrue(type, pageable)
                .map(profileMapper::toSummary);
    }

    public Page<ProfileSummaryResponse> getAllProfilesPaged(Pageable pageable) {
        return profileRepository.findByDeletedFalseAndApprovedTrue(pageable)
                .map(profileMapper::toSummary);
    }

    public List<ProfileSummaryResponse> getBatchMates(String department, int passingYear) {
        return profileRepository.findByDepartmentAndPassingYearAndDeletedFalse(department, passingYear)
                .stream().filter(ProfileDocument::isApproved).map(profileMapper::toSummary).toList();
    }

    // OPTIMIZED: single aggregation instead of 3 separate count queries
    @Cacheable(value = "profileCounts", unless = "#result == null")
    public Map<String, Long> getProfileCounts() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("deleted").is(false).and("approved").is(true)),
                Aggregation.group("profileType").count().as("count")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "profiles", Map.class);

        long alumni = 0, student = 0, faculty = 0;
        for (Map r : results.getMappedResults()) {
            String type = (String) r.get("_id");
            long count = ((Number) r.get("count")).longValue();
            if ("ALUMNI".equals(type)) alumni = count;
            else if ("STUDENT".equals(type)) student = count;
            else if ("FACULTY".equals(type)) faculty = count;
        }
        long total = alumni + student + faculty;
        return Map.of("alumni", alumni, "student", student, "faculty", faculty, "total", total);
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

        Role role = switch (newType) {
            case ALUMNI -> Role.ALUMNI;
            case STUDENT -> Role.STUDENT;
            case FACULTY -> Role.FACULTY;
        };
        authService.updateUserRole(userId, role);
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
        LocalDate now = LocalDate.now();
        if (passingYear < now.getYear() || (passingYear == now.getYear() && now.getMonthValue() >= 8))
            return ProfileType.ALUMNI;
        return ProfileType.STUDENT;
    }
}