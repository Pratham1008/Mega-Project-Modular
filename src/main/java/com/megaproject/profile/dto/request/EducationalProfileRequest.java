package com.megaproject.profile.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Set;

@Data
public class EducationalProfileRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email
    private String email;

    @NotBlank(message = "department is required")
    private String department;

    private String phone;
    private String bloodGroup;
    private String dateOfBirth;
    private String photoUrl;

    @Valid
    private AddressRequest address;

    @Valid
    private SocialsRequest socials;

    @NotBlank(message = "registrationNumber is required")
    private String registrationNumber;

    @NotNull(message = "admissionYear is required")
    @Min(1900)
    private Integer admissionYear;

    @NotNull(message = "passingYear is required")
    @Min(1900)
    private Integer passingYear;

    private Integer currentSemester;

    private String resumeUrl;

    private Set<String> skills;

    
    private String jobTitle;
    private String company;

    private String location;
}
