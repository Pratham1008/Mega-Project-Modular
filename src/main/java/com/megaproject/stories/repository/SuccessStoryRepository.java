package com.megaproject.stories.repository;

import com.megaproject.stories.model.SuccessStory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SuccessStoryRepository extends MongoRepository<SuccessStory, String> {
    List<SuccessStory> findByActiveTrueOrderByCreatedAtDesc();
}
