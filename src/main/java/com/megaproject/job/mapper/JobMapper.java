package com.megaproject.job.mapper;

import com.megaproject.job.dto.request.JobRequest;
import com.megaproject.job.dto.response.JobResponse;
import com.megaproject.job.model.Job;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface JobMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "postedByUserId", ignore = true)
    Job toJob(JobRequest req);

    JobResponse toJobResponse(Job job);

    List<JobResponse> toJobResponseList(List<Job> jobs);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "postedByUserId", ignore = true)
    void updateJob(JobRequest req, @MappingTarget Job job);
}
