package com.megaproject.stories.dto;

import lombok.*;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SuccessStoryResponse {
    private String id;
    private String title;
    private String description;
    private String personName;
    private String personDesignation;
    private String personBatch;
    private String personDepartment;
    private String personPhotoUrl;
    private String storyImageUrl;
    private String quote;
    private String createdByUserId;
    private Boolean active;
    private Instant createdAt;
}
