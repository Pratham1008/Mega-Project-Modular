package com.megaproject.stories.service;

import com.megaproject.stories.dto.SuccessStoryRequest;
import com.megaproject.stories.dto.SuccessStoryResponse;
import com.megaproject.stories.model.SuccessStory;
import com.megaproject.stories.repository.SuccessStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SuccessStoryService {

    private final SuccessStoryRepository repository;

    public SuccessStoryResponse create(SuccessStoryRequest req, String userId) {
        SuccessStory story = SuccessStory.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .personName(req.getPersonName())
                .personDesignation(req.getPersonDesignation())
                .personBatch(req.getPersonBatch())
                .personDepartment(req.getPersonDepartment())
                .personPhotoUrl(req.getPersonPhotoUrl())
                .storyImageUrl(req.getStoryImageUrl())
                .quote(req.getQuote())
                .createdByUserId(userId)
                .build();
        return toResponse(repository.save(story));
    }

    public List<SuccessStoryResponse> getAllActive() {
        return repository.findByActiveTrueOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    public SuccessStoryResponse getById(String id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Story not found: " + id)));
    }

    public void softDelete(String id, String userId, boolean isAdmin) {
        SuccessStory story = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Story not found: " + id));
        if (!isAdmin && !userId.equals(story.getCreatedByUserId())) {
            throw new RuntimeException("You can only delete stories you created.");
        }
        story.setActive(false);
        repository.save(story);
    }

    private SuccessStoryResponse toResponse(SuccessStory s) {
        return SuccessStoryResponse.builder()
                .id(s.getId())
                .title(s.getTitle())
                .description(s.getDescription())
                .personName(s.getPersonName())
                .personDesignation(s.getPersonDesignation())
                .personBatch(s.getPersonBatch())
                .personDepartment(s.getPersonDepartment())
                .personPhotoUrl(s.getPersonPhotoUrl())
                .storyImageUrl(s.getStoryImageUrl())
                .quote(s.getQuote())
                .createdByUserId(s.getCreatedByUserId())
                .active(s.getActive())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
