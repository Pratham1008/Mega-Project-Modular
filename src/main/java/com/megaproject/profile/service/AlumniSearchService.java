package com.megaproject.profile.service;

import com.megaproject.profile.dto.response.AlumniSearchResponse;
import com.megaproject.profile.mapper.ProfileMapper;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Alumni search service backed by MongoDB full-text search ($text index).
 *
 * The text index on ProfileDocument covers: fullName (weight 3),
 * department (weight 2), jobTitle (weight 2), company (weight 2),
 * location (weight 1), and skills (weight 1).
 *
 * MongoDB's $text search handles tokenization and stemming similar to
 * Elasticsearch for typical use-cases such as searching alumni by name,
 * company, skills, or department.
 *
 * NOTE: Make sure the text index is created on MongoDB before the first query.
 * Spring Data MongoDB will auto-create it if you enable auto-index-creation:
 *   spring.data.mongodb.auto-index-creation=true
 * Or create it manually:
 *   db.profiles.createIndex({ fullName:"text", jobTitle:"text", company:"text",
 *     location:"text", department:"text", skills:"text" },
 *     { weights: { fullName:3, department:2, jobTitle:2, company:2 }, name:"alumni_text_idx" })
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlumniSearchService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public List<AlumniSearchResponse> search(String query) {
        log.info("Alumni text search: query='{}'", query);
        return profileRepository.searchAlumniByText(query)
                .stream()
                .map(profileMapper::toAlumniSearchResponse)
                .toList();
    }
}
