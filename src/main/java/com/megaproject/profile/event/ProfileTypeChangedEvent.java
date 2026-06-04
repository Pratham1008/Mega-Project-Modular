package com.megaproject.profile.event;

public record ProfileTypeChangedEvent(
        String userId,
        String newRole
) {}
