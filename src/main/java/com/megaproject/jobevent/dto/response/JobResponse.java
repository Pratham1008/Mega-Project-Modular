package com.megaproject.jobevent.dto.response;

import com.megaproject.jobevent.model.ExperienceLevel;
import com.megaproject.jobevent.model.JobType;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JobResponse {
    private String id;
    private String title;
    private String companyName;
    private String location;
    private JobType jobType;
    private ExperienceLevel experienceLevel;
    private String description;
    private List<String> requirements;
    private List<String> skills;
    private Double salaryMin;
    private Double salaryMax;
    private String applyLink;
    private String contactEmail;
    private String postedByUserId;
    private Boolean active;
    private Instant createdAt;
    private LocalDate startDate;
    private LocalDate endDate;
}
