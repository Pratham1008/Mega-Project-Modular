package com.megaproject.jobevent.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Document(collection = "jobs")
@CompoundIndexes({
        @CompoundIndex(name = "company_active_idx", def = "{'companyName': 1, 'active': 1}"),
        @CompoundIndex(name = "skills_active_idx",  def = "{'skills': 1, 'active': 1}")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Job {

    @Id
    private String id;

    private String title;

    @Indexed
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
    
    private LocalDate startDate;
    private LocalDate endDate;

    @Builder.Default
    @Indexed
    private Boolean active = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
