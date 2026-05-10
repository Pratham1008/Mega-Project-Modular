package com.megaproject.jobevent.repository;

import com.megaproject.jobevent.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EventRepository extends MongoRepository<Event, String> {
    List<Event> findByActiveTrue();
    Page<Event> findByActiveTrue(Pageable pageable);
    List<Event> findByCreatedByUserIdAndActiveTrue(String createdByUserId);
}
