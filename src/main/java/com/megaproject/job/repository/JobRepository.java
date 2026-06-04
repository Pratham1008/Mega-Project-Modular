package com.megaproject.job.repository;

import com.megaproject.job.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends MongoRepository<Job, String> {
    List<Job> findByActiveTrue();
    Page<Job> findByActiveTrue(Pageable pageable);
    List<Job> findByCompanyNameIgnoreCaseAndActiveTrue(String companyName);
    List<Job> findByPostedByUserIdAndActiveTrue(String postedByUserId);
}
