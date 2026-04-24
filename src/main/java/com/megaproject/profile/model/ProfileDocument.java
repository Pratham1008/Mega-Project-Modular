package com.megaproject.profile.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TextScore;

import java.time.Instant;
import java.util.Set;

/**
 * Single consolidated MongoDB document for all profile types.
 * Replaces the old split JPA entities: Profile + ProfileEducationalData + ProfileFaculty.
 * The userId field is the same String ID that exists in the users collection.
 *
 * Text index covers fullName, jobTitle, company, location, department, skills
 * for full-text search (replaces Elasticsearch alumni search).
 */
@Document(collection = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileDocument {

    @Id
    private String id;

    /** References users._id — not a DB-level foreign key, just a logical link */
    @Indexed(unique = true)
    private String userId;

    @Indexed(unique = true)
    private String email;

    @TextIndexed(weight = 3)
    private String fullName;

    private String phone;

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

    // ---- Educational / Alumni fields (used when profileType = STUDENT or ALUMNI) ----
    @Indexed(unique = true, sparse = true)
    private String registrationNumber;
    private Integer admissionYear;
    private Integer passingYear;
    private Integer currentSemester;
    private String resumeUrl;
    private Set<String> skills;

    // ---- Employment info (mainly for ALUMNI) ----
    @TextIndexed(weight = 2)
    private String jobTitle;

    @TextIndexed(weight = 2)
    private String company;

    @TextIndexed
    private String location;

    // ---- Faculty-specific fields ----
    private String designation;
    private String officeLocation;
    private String researchInterests;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
