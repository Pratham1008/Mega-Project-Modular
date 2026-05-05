package com.megaproject.profile.repository;

import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.model.ProfileType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends MongoRepository<ProfileDocument, String> {

    Optional<ProfileDocument> findByUserId(String userId);

    Optional<ProfileDocument> findByEmail(String email);

    boolean existsByUserId(String userId);

    boolean existsByRegistrationNumber(String registrationNumber);

    List<ProfileDocument> findByProfileTypeAndDeletedFalse(ProfileType profileType);

    List<ProfileDocument> findByProfileTypeAndDeletedFalseAndPassingYearLessThanEqual(ProfileType profileType, int passingYear);

    List<ProfileDocument> findByDeletedFalse();

    long countByProfileTypeAndDeletedFalseAndApprovedTrue(ProfileType profileType);

    /**
     * Full-text search on alumni profiles using MongoDB $text index.
     * The text index covers: fullName, jobTitle, company, location, department, skills.
     */
    @Query("{ $text: { $search: ?0 }, profileType: 'ALUMNI', deleted: false }")
    List<ProfileDocument> searchAlumniByText(String query);
}
