package com.megaproject.profile.service;

import com.megaproject.auth.model.Role;
import com.megaproject.auth.service.AuthService;
import com.megaproject.profile.dto.request.EducationalProfileRequest;
import com.megaproject.profile.dto.response.EducationalProfileResponse;
import com.megaproject.profile.exception.ProfileAlreadyExistsException;
import com.megaproject.profile.exception.ProfileNotFoundException;
import com.megaproject.profile.mapper.ProfileMapper;
import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.model.ProfileType;
import com.megaproject.profile.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProfileMapper profileMapper;

    @Mock
    private AuthService authService;

    @InjectMocks
    private ProfileService profileService;

    private EducationalProfileRequest req;
    private ProfileDocument doc;
    private EducationalProfileResponse resp;

    @BeforeEach
    void setUp() {
        req = new EducationalProfileRequest();
        req.setUserId("user123");
        req.setRegistrationNumber("REG123");

        doc = new ProfileDocument();
        doc.setUserId("user123");

        resp = new EducationalProfileResponse();
        resp.setUserId("user123");
    }

    @Test
    void testCreateEducationalProfile_Alumni() {
        req.setPassingYear(Year.now().getValue() - 1);
        
        when(profileRepository.existsByUserId("user123")).thenReturn(false);
        when(profileRepository.existsByRegistrationNumber("REG123")).thenReturn(false);
        when(profileMapper.toDocument(req)).thenReturn(doc);
        when(profileRepository.save(any(ProfileDocument.class))).thenAnswer(i -> {
            ProfileDocument d = i.getArgument(0);
            return d;
        });
        when(profileMapper.toEducationalResponse(any(ProfileDocument.class))).thenReturn(resp);

        EducationalProfileResponse result = profileService.createEducationalProfile(req);

        assertEquals(ProfileType.ALUMNI, doc.getProfileType());
        verify(authService).updateUserRole("user123", Role.ALUMNI);
        assertNotNull(result);
    }

    @Test
    void testCreateEducationalProfile_Student() {
        req.setPassingYear(Year.now().getValue() + 1);

        when(profileRepository.existsByUserId("user123")).thenReturn(false);
        when(profileRepository.existsByRegistrationNumber("REG123")).thenReturn(false);
        when(profileMapper.toDocument(req)).thenReturn(doc);
        when(profileRepository.save(any(ProfileDocument.class))).thenAnswer(i -> i.getArgument(0));
        when(profileMapper.toEducationalResponse(any(ProfileDocument.class))).thenReturn(resp);

        profileService.createEducationalProfile(req);

        assertEquals(ProfileType.STUDENT, doc.getProfileType());
        verify(authService).updateUserRole("user123", Role.STUDENT);
    }

    @Test
    void testCreateEducationalProfile_DuplicateUserId() {
        when(profileRepository.existsByUserId("user123")).thenReturn(true);
        assertThrows(ProfileAlreadyExistsException.class, () -> profileService.createEducationalProfile(req));
        verify(profileRepository, never()).save(any());
    }

    @Test
    void testGetProfileCounts() {
        when(profileRepository.countByProfileTypeAndDeletedFalseAndApprovedTrue(ProfileType.ALUMNI)).thenReturn(10L);
        when(profileRepository.countByProfileTypeAndDeletedFalseAndApprovedTrue(ProfileType.STUDENT)).thenReturn(5L);
        when(profileRepository.countByProfileTypeAndDeletedFalseAndApprovedTrue(ProfileType.FACULTY)).thenReturn(2L);

        Map<String, Long> counts = profileService.getProfileCounts();

        assertEquals(10L, counts.get("alumni"));
        assertEquals(5L, counts.get("student"));
        assertEquals(2L, counts.get("faculty"));
        assertEquals(17L, counts.get("total"));
    }

    @Test
    void testApproveProfile() {
        when(profileRepository.findByUserId("user123")).thenReturn(Optional.of(doc));
        when(profileRepository.save(doc)).thenReturn(doc);
        when(profileMapper.toEducationalResponse(doc)).thenReturn(resp);

        profileService.approveProfile("user123");

        assertTrue(doc.isApproved());
        verify(profileRepository).save(doc);
    }

    @Test
    void testDeleteProfile() {
        when(profileRepository.findByUserId("user123")).thenReturn(Optional.of(doc));
        
        profileService.deleteProfile("user123");

        assertTrue(doc.isDeleted());
        verify(profileRepository).save(doc);
    }

    @Test
    void testGetDocumentByUserId_NotFound() {
        when(profileRepository.findByUserId("not_found")).thenReturn(Optional.empty());
        assertThrows(ProfileNotFoundException.class, () -> profileService.getEducationalProfile("not_found"));
    }
}
