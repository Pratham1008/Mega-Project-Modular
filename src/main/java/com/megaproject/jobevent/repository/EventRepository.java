package com.megaproject.jobevent.repository;

import com.megaproject.jobevent.model.Event;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EventRepository extends MongoRepository<Event, String> {
    List<Event> findByActiveTrue();
    List<Event> findByCreatedByUserIdAndActiveTrue(String createdByUserId);
}
