package com.megaproject.jobevent.dto.request;

import com.megaproject.jobevent.model.ExperienceLevel;
import com.megaproject.jobevent.model.JobType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.time.LocalDate;

@Data
public class JobRequest {
    @NotBlank private String title;
    @NotBlank private String companyName;
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
    private LocalDate startDate;
    private LocalDate endDate;
}
