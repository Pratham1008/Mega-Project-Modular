package com.megaproject.profile.service;

import com.megaproject.profile.dto.response.AlumniSearchResponse;
import com.megaproject.profile.mapper.ProfileMapper;
import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.model.ProfileType;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlumniSearchService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final MongoTemplate mongoTemplate;

    public Page<AlumniSearchResponse> searchWithFilters(
            String query, String department, String company, Integer passingYear, String location,
            int page, int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        boolean hasQuery = query != null && !query.isBlank();
        boolean hasFilters = (department != null && !department.isBlank())
                || (company != null && !company.isBlank())
                || passingYear != null
                || (location != null && !location.isBlank());

        Query mongoQuery;

        if (hasQuery && !hasFilters) {
            // Pure text search — use $text index with sort by score
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matchingPhrase(query);
            mongoQuery = TextQuery.queryText(textCriteria).sortByScore();
            mongoQuery.addCriteria(Criteria.where("profileType").in(ProfileType.ALUMNI, ProfileType.STUDENT));
            mongoQuery.addCriteria(Criteria.where("deleted").is(false));
            mongoQuery.addCriteria(Criteria.where("approved").is(true));
        } else {
            // Filters present (possibly with text query)
            List<Criteria> criteriaList = new ArrayList<>();
            criteriaList.add(Criteria.where("profileType").in(ProfileType.ALUMNI, ProfileType.STUDENT));
            criteriaList.add(Criteria.where("deleted").is(false));
            criteriaList.add(Criteria.where("approved").is(true));

            if (hasQuery) {
                // Use $text search for the query part alongside filter criteria
                TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matchingPhrase(query);
                mongoQuery = TextQuery.queryText(textCriteria).sortByScore();
            } else {
                mongoQuery = new Query();
            }

            if (department != null && !department.isBlank())
                criteriaList.add(Criteria.where("department").regex(department, "i"));
            if (company != null && !company.isBlank())
                criteriaList.add(Criteria.where("company").regex(company, "i"));
            if (passingYear != null)
                criteriaList.add(Criteria.where("passingYear").is(passingYear));
            if (location != null && !location.isBlank())
                criteriaList.add(Criteria.where("location").regex(location, "i"));

            for (Criteria c : criteriaList) {
                mongoQuery.addCriteria(c);
            }
        }

        // Field projection for efficiency
        mongoQuery.fields()
                .include("userId", "fullName", "photoUrl", "department", "passingYear",
                        "profileType", "company", "jobTitle", "location", "skills", "socials");

        // Get total count for pagination
        long total = mongoTemplate.count(Query.of(mongoQuery).limit(-1).skip(-1), ProfileDocument.class);

        // Apply pagination
        mongoQuery.with(pageable);

        List<AlumniSearchResponse> results = mongoTemplate.find(mongoQuery, ProfileDocument.class)
                .stream().map(profileMapper::toAlumniSearchResponse).toList();

        return new PageImpl<>(results, pageable, total);
    }
}