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

        Query mongoQuery = new Query();
        List<Criteria> criteriaList = new java.util.ArrayList<>();

        criteriaList.add(Criteria.where("profileType").in(ProfileType.ALUMNI, ProfileType.STUDENT));
        criteriaList.add(Criteria.where("deleted").is(false));
        criteriaList.add(Criteria.where("approved").is(true));

        if (query != null && !query.isBlank()) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("fullName").regex(query, "i"),
                    Criteria.where("company").regex(query, "i"),
                    Criteria.where("jobTitle").regex(query, "i"),
                    Criteria.where("department").regex(query, "i"),
                    Criteria.where("skills").regex(query, "i")
            ));
        }

        if (department != null && !department.isBlank()) {
            criteriaList.add(Criteria.where("department").regex(department, "i"));
        }
        if (company != null && !company.isBlank()) {
            criteriaList.add(Criteria.where("company").regex(company, "i"));
        }
        if (passingYear != null) {
            criteriaList.add(Criteria.where("passingYear").is(passingYear));
        }
        if (location != null && !location.isBlank()) {
            criteriaList.add(Criteria.where("location").regex(location, "i"));
        }

        Criteria finalCriteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        mongoQuery.addCriteria(finalCriteria);
        mongoQuery.limit(100);

        List<ProfileDocument> results = mongoTemplate.find(mongoQuery, ProfileDocument.class);
        return results.stream().map(profileMapper::toAlumniSearchResponse).toList();
    }
}
