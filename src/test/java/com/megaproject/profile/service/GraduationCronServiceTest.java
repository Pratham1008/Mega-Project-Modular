package com.megaproject.profile.service;

import com.megaproject.auth.model.Role;
import com.megaproject.auth.service.AuthService;
import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.model.ProfileType;
import com.megaproject.profile.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraduationCronServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private GraduationCronService graduationCronService;

    @Test
    void testPromoteStudents_updatesProfileTypeAndRole() {
        ProfileDocument p1 = new ProfileDocument();
        p1.setUserId("user1");
        p1.setProfileType(ProfileType.STUDENT);

        ProfileDocument p2 = new ProfileDocument();
        p2.setUserId("user2");
        p2.setProfileType(ProfileType.STUDENT);

        when(profileRepository.findByProfileTypeAndDeletedFalseAndPassingYearLessThan(
                eq(ProfileType.STUDENT), anyInt())).thenReturn(List.of(p1, p2));

        graduationCronService.promoteStudentsToAlumni();

        assertEquals(ProfileType.ALUMNI, p1.getProfileType());
        assertEquals(ProfileType.ALUMNI, p2.getProfileType());

        verify(profileRepository, times(2)).save(any(ProfileDocument.class));
        verify(authService).updateUserRole("user1", Role.ALUMNI);
        verify(authService).updateUserRole("user2", Role.ALUMNI);
    }

    @Test
    void testPromoteStudents_noEligible() {
        when(profileRepository.findByProfileTypeAndDeletedFalseAndPassingYearLessThan(
                eq(ProfileType.STUDENT), anyInt())).thenReturn(List.of());

        graduationCronService.promoteStudentsToAlumni();

        verify(profileRepository, never()).save(any());
        verify(authService, never()).updateUserRole(anyString(), any());
    }
}
