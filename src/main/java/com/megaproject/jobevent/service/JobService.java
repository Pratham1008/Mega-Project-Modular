package com.megaproject.jobevent.service;

import com.megaproject.jobevent.dto.request.JobRequest;
import com.megaproject.jobevent.dto.response.JobResponse;
import com.megaproject.jobevent.exception.ResourceNotFoundException;
import com.megaproject.jobevent.mapper.JobEventMapper;
import com.megaproject.jobevent.model.Job;
import com.megaproject.jobevent.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobEventMapper mapper;

    public JobResponse create(JobRequest req, String postedByUserId) {
        Job job = mapper.toJob(req);
        job.setActive(true);
        job.setPostedByUserId(postedByUserId);
        return mapper.toJobResponse(jobRepository.save(job));
    }

    public JobResponse getById(String id) {
        return mapper.toJobResponse(findById(id));
    }

    public List<JobResponse> getAllActive() {
        return mapper.toJobResponseList(jobRepository.findByActiveTrue());
    }

    public List<JobResponse> getByCreator(String userId) {
        return mapper.toJobResponseList(
                jobRepository.findByPostedByUserIdAndActiveTrue(userId));
    }

    public List<JobResponse> getByCompany(String companyName) {
        return mapper.toJobResponseList(
                jobRepository.findByCompanyNameIgnoreCaseAndActiveTrue(companyName));
    }

    public JobResponse update(String id, JobRequest req, String requesterId, String role) {
        Job job = findById(id);
        if (!job.getPostedByUserId().equals(requesterId) && !"ADMIN".equals(role)) {
            throw new AccessDeniedException("You can only edit your own job posts");
        }
        mapper.updateJob(req, job);
        return mapper.toJobResponse(jobRepository.save(job));
    }

    public void softDelete(String id) {
        Job job = findById(id);
        job.setActive(false);
        jobRepository.save(job);
    }

    private Job findById(String id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));
    }
}
