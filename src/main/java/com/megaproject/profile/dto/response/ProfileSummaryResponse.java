package com.megaproject.profile.dto.response;
import com.megaproject.profile.model.ProfileType;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProfileSummaryResponse {
    private String id;
    private String userId;
    private String fullName;
    private String email;
    private String department;
    private String photoUrl;
    private ProfileType profileType;
    private boolean approved;
}
