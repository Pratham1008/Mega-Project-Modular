package com.megaproject.profile.service;

import com.megaproject.profile.dto.response.AlumniSearchResponse;
import com.megaproject.profile.mapper.ProfileMapper;
import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.model.ProfileType;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlumniSearchService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final MongoTemplate mongoTemplate;

    public List<AlumniSearchResponse> search(String query) {
        return profileRepository.searchAlumniByText(query)
                .stream()
                .map(profileMapper::toAlumniSearchResponse)
                .toList();
    }

    public List<AlumniSearchResponse> searchWithFilters(
            String query, String department, String company, Integer passingYear, String location) {

        Query mongoQuery;

        if (query != null && !query.isBlank()) {
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matchingPhrase(query);
            mongoQuery = TextQuery.queryText(textCriteria).sortByScore();
        } else {
            mongoQuery = new Query();
        }

        Criteria criteria = Criteria.where("profileType").is(ProfileType.ALUMNI)
                .and("deleted").is(false)
                .and("approved").is(true);

        if (department != null && !department.isBlank()) {
            criteria = criteria.and("department").regex(department, "i");
        }
        if (company != null && !company.isBlank()) {
            criteria = criteria.and("company").regex(company, "i");
        }
        if (passingYear != null) {
            criteria = criteria.and("passingYear").is(passingYear);
        }
        if (location != null && !location.isBlank()) {
            criteria = criteria.and("location").regex(location, "i");
        }

        mongoQuery.addCriteria(criteria);
        mongoQuery.limit(100);

        List<ProfileDocument> results = mongoTemplate.find(mongoQuery, ProfileDocument.class);
        return results.stream().map(profileMapper::toAlumniSearchResponse).toList();
    }
}
