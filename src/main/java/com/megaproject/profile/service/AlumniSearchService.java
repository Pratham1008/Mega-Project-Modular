package com.megaproject.profile.service;

import com.megaproject.profile.dto.response.AlumniSearchResponse;
import com.megaproject.profile.mapper.ProfileMapper;
import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.model.ProfileType;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlumniSearchService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final MongoTemplate mongoTemplate;

    public Page<AlumniSearchResponse> searchWithFilters(
            String query, String department, String company,
            Integer passingYear, String location, int page, int size) {

        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, safeSize);

        boolean hasQuery   = query != null && !query.isBlank();
        boolean hasDept    = department != null && !department.isBlank();
        boolean hasCo      = company    != null && !company.isBlank();
        boolean hasYear    = passingYear != null;
        boolean hasLoc     = location   != null && !location.isBlank();
        boolean hasFilters = hasDept || hasCo || hasYear || hasLoc;

        Query mongoQuery = hasQuery
                ? TextQuery.queryText(TextCriteria.forDefaultLanguage().matchingPhrase(query)).sortByScore()
                : new Query();

        mongoQuery.addCriteria(Criteria.where("profileType").in(ProfileType.ALUMNI, ProfileType.STUDENT)
                .and("deleted").is(false)
                .and("approved").is(true));

        if (hasDept) {
            mongoQuery.addCriteria(Criteria.where("department").is(department.trim()));
        }
        if (hasYear) {
            mongoQuery.addCriteria(Criteria.where("passingYear").is(passingYear));
        }
        if (hasCo) {
            mongoQuery.addCriteria(Criteria.where("company")
                    .regex("^" + escapeRegex(company.trim()), "i"));
        }
        if (hasLoc) {
            mongoQuery.addCriteria(Criteria.where("location")
                    .regex(escapeRegex(location.trim()), "i"));
        }

        mongoQuery.fields()
                .include("userId","fullName","photoUrl","department","passingYear",
                        "profileType","company","jobTitle","location","skills","socials");

        long total = mongoTemplate.count(
                Query.of(mongoQuery).limit(-1).skip(-1), ProfileDocument.class);

        mongoQuery.with(pageable);
        List<AlumniSearchResponse> results = mongoTemplate.find(mongoQuery, ProfileDocument.class)
                .stream().map(profileMapper::toAlumniSearchResponse).toList();

        return new PageImpl<>(results, pageable, total);
    }

    private static String escapeRegex(String s) {
        return s.replaceAll("([\\\\.*+?^${}()|\\[\\]])", "\\\\$1");
    }
}