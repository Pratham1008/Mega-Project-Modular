package com.megaproject.profile.service;

import com.megaproject.common.util.PasswordGeneratorUtil;
import com.megaproject.profile.dto.request.EducationalProfileRequest;
import com.megaproject.profile.dto.request.FacultyProfileRequest;
import com.megaproject.profile.dto.response.EducationalProfileResponse;
import com.megaproject.profile.dto.response.FacultyProfileResponse;
import com.megaproject.profile.event.FacultyProvisionedEvent;
import com.megaproject.profile.event.ProfileTypeChangedEvent;
import com.megaproject.profile.exception.ProfileAlreadyExistsException;
import com.megaproject.profile.exception.ProfileNotFoundException;
import com.megaproject.profile.mapper.ProfileMapper;
import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.model.ProfileType;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileCommandService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final ProfileLookupService profileLookupService;
    private final PasswordGeneratorUtil passwordGenerator;
    private final ApplicationEventPublisher eventPublisher;

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

        String role = saved.getProfileType() == ProfileType.ALUMNI ? "ALUMNI" : "STUDENT";
        eventPublisher.publishEvent(new ProfileTypeChangedEvent(saved.getUserId(), role));

        return profileMapper.toEducationalResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.subject")
    public EducationalProfileResponse updateEducationalProfile(
            String userId, EducationalProfileRequest req) {

        ProfileDocument doc = getDocumentByUserId(userId);
        profileMapper.updateDocumentFromRequest(req, doc);
        doc.setProfileType(determineType(req.getPassingYear()));
        ProfileDocument saved = profileRepository.save(doc);

        String role = saved.getProfileType() == ProfileType.ALUMNI ? "ALUMNI" : "STUDENT";
        eventPublisher.publishEvent(new ProfileTypeChangedEvent(userId, role));

        return profileMapper.toEducationalResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public FacultyProfileResponse createFacultyProfile(FacultyProfileRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (profileRepository.existsByEmail(email))
            throw new ProfileAlreadyExistsException(
                    "Profile already exists for email: " + email);

        String userId = profileLookupService.findOrProvisionFacultyUser(
                email, req.getFullName(), passwordGenerator.generate());

        ProfileDocument doc = profileMapper.toDocument(req);
        doc.setUserId(userId);
        doc.setEmail(email);
        doc.setProfileType(ProfileType.FACULTY);
        doc.setApproved(true);
        ProfileDocument saved = profileRepository.save(doc);

        eventPublisher.publishEvent(new ProfileTypeChangedEvent(saved.getUserId(), "FACULTY"));

        return profileMapper.toFacultyResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public FacultyProfileResponse updateFacultyProfile(String userId, FacultyProfileRequest req) {
        ProfileDocument doc = getDocumentByUserId(userId);
        profileMapper.updateDocumentFromRequest(req, doc);
        return profileMapper.toFacultyResponse(profileRepository.save(doc));
    }

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

        String role = switch (newType) {
            case ALUMNI -> "ALUMNI";
            case STUDENT -> "STUDENT";
            case FACULTY -> "FACULTY";
        };
        eventPublisher.publishEvent(new ProfileTypeChangedEvent(userId, role));
        return profileMapper.toEducationalResponse(saved);
    }

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
