package com.megaproject.profile.dto.response;

import com.megaproject.profile.model.ProfileType;
import lombok.*;
import java.util.List;

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
    private String specialization;
    private List<String> degrees;
    private Integer publications;
    private boolean approved;
}
