package com.megaproject.profile.dto.response;
import lombok.*;
import java.util.Set;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AlumniSearchResponse {
    private String userId;
    private String fullName;
    private String email;
    private String profileType;
    private String jobTitle;
    private String company;
    private String location;
    private String department;
    private Integer passingYear;
    private Set<String> skills;
}
