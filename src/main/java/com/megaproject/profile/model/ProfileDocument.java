package com.megaproject.profile.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

@Document(collection = "profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfileDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    @Indexed(unique = true)
    private String email;

    @TextIndexed(weight = 3)
    private String fullName;

    private String phone;
    private String bloodGroup;
    private String dateOfBirth;

    @TextIndexed(weight = 2)
    private String department;

    private String photoUrl;

    @Indexed
    private ProfileType profileType;

    private Address address;
    private Socials socials;

    @Builder.Default
    private boolean approved = false;

    @Builder.Default
    private boolean deleted = false;

    @Indexed(unique = true, sparse = true)
    private String registrationNumber;
    private Integer admissionYear;

    @Indexed
    private Integer passingYear;
    private Integer currentSemester;
    private String resumeUrl;
    private Set<String> skills;

    @TextIndexed(weight = 2)
    private String jobTitle;

    @TextIndexed(weight = 2)
    private String company;

    @TextIndexed
    private String location;

    private String designation;
    private String officeLocation;
    private String researchInterests;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
