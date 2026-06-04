package com.megaproject.event.dto.response;

import com.megaproject.event.model.EventType;
import lombok.*;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EventResponse {
    private String id;
    private String title;
    private String description;
    private EventType eventType;
    private String venueName;
    private String venueAddress;
    private String meetingLink;
    private Instant startTime;
    private Instant endTime;
    private String bannerImageUrl;
    private Integer maxParticipants;
    private String registrationLink;
    private String targetAudience;
    private String createdByUserId;
    private Boolean active;
    private Instant createdAt;
    private int registeredCount;
    private java.util.List<String> registeredUserIds;
}
