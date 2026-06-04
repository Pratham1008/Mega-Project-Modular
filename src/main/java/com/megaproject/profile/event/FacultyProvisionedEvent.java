package com.megaproject.profile.event;

public record FacultyProvisionedEvent(
        String email,
        String fullName,
        String generatedPassword
) {}
