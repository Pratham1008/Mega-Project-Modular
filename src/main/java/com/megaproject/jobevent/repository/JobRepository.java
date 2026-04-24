package com.megaproject.jobevent.repository;

import com.megaproject.jobevent.model.Job;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends MongoRepository<Job, String> {
    List<Job> findByActiveTrue();
    List<Job> findByCompanyNameIgnoreCaseAndActiveTrue(String companyName);
    List<Job> findByPostedByUserIdAndActiveTrue(String postedByUserId);
}
