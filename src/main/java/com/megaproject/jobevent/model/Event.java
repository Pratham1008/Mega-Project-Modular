package com.megaproject.jobevent.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "events")
@CompoundIndexes({
        @CompoundIndex(name = "start_active_idx", def = "{'startTime': 1, 'isActive': 1}")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Event {

    @Id
    private String id;

    private String title;
    private String description;

    private EventType eventType;

    private String venueName;
    private String venueAddress;
    private String meetingLink;

    @Indexed
    private Instant startTime;
    private Instant endTime;

    private String organizerName;
    private String organizerContact;
    private String bannerImageUrl;

    private Integer maxParticipants;
    private List<String> registeredUserIds;
    private String registrationLink;
    private String targetAudience;   // ALL | STUDENTS_ONLY | ALUMNI_ONLY
    private String createdByUserId;

    @Builder.Default
    @Indexed
    private Boolean active = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
