package com.megaproject.profile.dto.response;

import com.megaproject.profile.model.ProfileType;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FacultyProfileResponse {
    private String id;
    private String userId;
    private String fullName;
    private String email;
    private String department;
    private String phone;
    private String photoUrl;
    private ProfileType profileType;
    private AddressResponse address;
    private SocialsResponse socials;
    private String designation;
    private String officeLocation;
    private String researchInterests;
    private boolean approved;
}
