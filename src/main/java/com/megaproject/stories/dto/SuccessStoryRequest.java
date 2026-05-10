package com.megaproject.stories.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SuccessStoryRequest {
    @NotBlank private String title;
    @NotBlank private String description;
    @NotBlank private String personName;
    private String personDesignation;
    private String personBatch;
    private String personDepartment;
    private String personPhotoUrl;
    private String storyImageUrl;
    private String quote;
}
