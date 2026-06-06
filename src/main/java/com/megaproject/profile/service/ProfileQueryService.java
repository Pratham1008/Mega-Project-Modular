package com.megaproject.profile.service;

import com.megaproject.profile.dto.response.ProfileMapResponse;
import com.megaproject.profile.dto.response.ProfileSummaryResponse;
import com.megaproject.profile.dto.response.EducationalProfileResponse;
import com.megaproject.profile.dto.response.FacultyProfileResponse;
import com.megaproject.profile.exception.ProfileNotFoundException;
import com.megaproject.profile.mapper.ProfileMapper;
import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.model.ProfileType;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProfileQueryService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final MongoTemplate mongoTemplate;

    public EducationalProfileResponse getEducationalProfile(String userId) {
        return profileMapper.toEducationalResponse(getDocumentByUserId(userId));
    }

    public FacultyProfileResponse getFacultyProfile(String userId) {
        return profileMapper.toFacultyResponse(getDocumentByUserId(userId));
    }

    public List<ProfileSummaryResponse> getProfilesByType(ProfileType type) {
        return getProfilesByTypePaged(type, PageRequest.of(0, 500)).getContent();
    }

    public List<ProfileSummaryResponse> getAllProfiles() {
        return getAllProfilesPaged(PageRequest.of(0, 500)).getContent();
    }

    public Page<ProfileSummaryResponse> getProfilesByTypePaged(ProfileType type, Pageable pageable) {
        return profileRepository.findByProfileTypeAndDeletedFalseAndApprovedTrue(type, pageable)
                .map(profileMapper::toSummary);
    }

    public Page<ProfileSummaryResponse> getAllProfilesPaged(Pageable pageable) {
        return profileRepository.findByDeletedFalseAndApprovedTrue(pageable)
                .map(profileMapper::toSummary);
    }

    public List<ProfileSummaryResponse> getBatchMates(String department, int passingYear) {
        return profileRepository.findByDepartmentAndPassingYearAndDeletedFalse(department, passingYear)
                .stream().filter(ProfileDocument::isApproved).map(profileMapper::toSummary).toList();
    }

    public Page<ProfileSummaryResponse> getBatchMatesPaged(String department, int passingYear, Pageable pageable) {
        return profileRepository
                .findByDepartmentAndPassingYearAndDeletedFalseAndApprovedTrue(department, passingYear, pageable)
                .map(profileMapper::toSummary);
    }

        public Page<ProfileMapResponse> getMapProfiles(ProfileType type, Pageable pageable) {
        return profileRepository.findProfilesWithLocationPaged(type, pageable)
                .map(this::toMapResponse);
    }

    @Cacheable(value = "profileCounts", unless = "#result == null")
    public Map<String, Long> getProfileCounts() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("deleted").is(false).and("approved").is(true)),
                Aggregation.group("profileType").count().as("count")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "profiles", Map.class);

        long alumni = 0, student = 0, faculty = 0;
        for (Map r : results.getMappedResults()) {
            String type = (String) r.get("_id");
            long count = ((Number) r.get("count")).longValue();
            if ("ALUMNI".equals(type)) alumni = count;
            else if ("STUDENT".equals(type)) student = count;
            else if ("FACULTY".equals(type)) faculty = count;
        }
        long total = alumni + student + faculty;
        return Map.of("alumni", alumni, "student", student, "faculty", faculty, "total", total);
    }

    

    private ProfileDocument getDocumentByUserId(String userId) {
        return profileRepository.findByUserId(userId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Profile not found for userId: " + userId));
    }

    private ProfileMapResponse toMapResponse(ProfileDocument p) {
        return new ProfileMapResponse(
                p.getUserId() != null ? p.getUserId() : "",
                p.getFullName() != null ? p.getFullName() : "",
                p.getPhotoUrl() != null ? p.getPhotoUrl() : "",
                p.getLocation() != null ? p.getLocation() : "",
                p.getDepartment() != null ? p.getDepartment() : "",
                p.getProfileType() != null ? p.getProfileType().name() : "",
                p.getCompany() != null ? p.getCompany() : "",
                p.getPassingYear() != null ? p.getPassingYear() : 0
        );
    }
}
