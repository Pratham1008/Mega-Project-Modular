package com.megaproject.profile.service;

import com.megaproject.auth.model.Role;
import com.megaproject.auth.service.AuthService;
import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.model.ProfileType;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraduationCronService {

    private final ProfileRepository profileRepository;
    private final AuthService authService;

    @Scheduled(cron = "0 0 0 1 8 *")
    public void promoteStudentsToAlumni() {
        int currentYear = Year.now().getValue();

        List<ProfileDocument> eligibleStudents = profileRepository
                .findByProfileTypeAndDeletedFalseAndPassingYearLessThan(ProfileType.STUDENT, currentYear);

        if (eligibleStudents.isEmpty()) {
            return;
        }

        int count = 0;
        for (ProfileDocument student : eligibleStudents) {
            try {
                student.setProfileType(ProfileType.ALUMNI);
                profileRepository.save(student);
                authService.updateUserRole(student.getUserId(), Role.ALUMNI);
                count++;
            } catch (Exception e) {
                log.error("Failed to promote user {} to ALUMNI", student.getUserId(), e);
            }
        }
    }
}
