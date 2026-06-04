package com.megaproject.event.dto.request;

import com.megaproject.event.model.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class EventRequest {
    @NotBlank private String title;
    private String description;
    @NotNull  private EventType eventType;
    private String venueName;
    private String venueAddress;
    private String meetingLink;
    @NotNull  private Instant startTime;
    private Instant endTime;
    private Integer maxParticipants;
    private String bannerImageUrl;
    private String registrationLink;
    private String targetAudience;   
}
