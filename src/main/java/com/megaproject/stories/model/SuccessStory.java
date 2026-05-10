package com.megaproject.stories.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "success_stories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SuccessStory {

    @Id
    private String id;

    private String title;
    private String description;

    /** The featured person's details */
    private String personName;
    private String personDesignation;
    private String personBatch;        // e.g. "Class of 2015"
    private String personDepartment;
    private String personPhotoUrl;

    /** Optional banner/story image */
    private String storyImageUrl;

    /** Quote from the person */
    private String quote;

    private String createdByUserId;

    @Builder.Default
    @Indexed
    private Boolean active = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
