package com.megaproject.profile.dto.response;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SocialsResponse {
    private String linkedinUrl, githubUrl, instagramUrl;
}
