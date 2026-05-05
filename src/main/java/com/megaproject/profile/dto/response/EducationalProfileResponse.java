package com.megaproject.profile.dto.response;

import com.megaproject.profile.model.ProfileType;
import lombok.*;

import java.util.Set;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EducationalProfileResponse {
    private String id;
    private String userId;
    private String fullName;
    private String email;
    private String department;
    private String phone;
    private String bloodGroup;
    private String dateOfBirth;
    private String photoUrl;
    private ProfileType profileType;
    private AddressResponse address;
    private SocialsResponse socials;
    private String registrationNumber;
    private Integer admissionYear;
    private Integer passingYear;
    private Integer currentSemester;
    private String resumeUrl;
    private Set<String> skills;
    private String jobTitle;
    private String company;
    private String location;
    private boolean approved;
}
