package com.megaproject.profile.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class FacultyProfileRequest {

    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email
    private String email;

    @NotBlank(message = "department is required")
    private String department;

    private String phone;
    private String photoUrl;

    @Valid
    private AddressRequest address;

    @Valid
    private SocialsRequest socials;

    
    private String designation;
    private String officeLocation;
    private String specialization;
    private List<String> degrees;
    private Integer publications;
}
