package com.megaproject.profile.service;

import com.megaproject.auth.model.Role;
import com.megaproject.auth.model.User;
import com.megaproject.auth.repository.UserRepository;
import com.megaproject.profile.event.FacultyProvisionedEvent;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileLookupService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public Optional<String> getFullName(String userId) {
        return profileRepository.findByUserId(userId)
                .map(p -> p.getFullName());
    }

    public Optional<String> getPhotoUrl(String userId) {
        return profileRepository.findByUserId(userId)
                .map(p -> p.getPhotoUrl());
    }

    public record ProfileSummary(String fullName, String photoUrl) {}

    public Optional<ProfileSummary> getNameAndPhoto(String userId) {
        return profileRepository.findByUserId(userId)
                .map(p -> new ProfileSummary(p.getFullName(), p.getPhotoUrl()));
    }

    public String findOrProvisionFacultyUser(String email, String fullName, String generatedPassword) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .password(passwordEncoder.encode(generatedPassword))
                            .role(Role.FACULTY)
                            .verified(true)
                            .build();
                    User saved = userRepository.save(newUser);
                    eventPublisher.publishEvent(new FacultyProvisionedEvent(email, fullName, generatedPassword));
                    log.info("Provisioned new Faculty account: {} (credentials sent via email)", email);
                    return saved.getId();
                });
    }
}

