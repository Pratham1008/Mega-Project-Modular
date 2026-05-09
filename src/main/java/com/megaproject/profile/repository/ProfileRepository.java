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

    boolean existsByEmail(String email);

    List<ProfileDocument> findByProfileTypeAndDeletedFalse(ProfileType profileType);

    List<ProfileDocument> findByProfileTypeAndDeletedFalseAndPassingYearLessThan(ProfileType profileType, int passingYear);

    @Query("{ 'profileType': ?0, 'deleted': false, 'location': { $exists: true, $nin: ['', null] } }")
    List<ProfileDocument> findProfilesWithLocation(ProfileType type);

    List<ProfileDocument> findByDeletedFalse();

    long countByProfileTypeAndDeletedFalseAndApprovedTrue(ProfileType profileType);

    @Query("{ $text: { $search: ?0 }, profileType: 'ALUMNI', deleted: false }")
    List<ProfileDocument> searchAlumniByText(String query);
}
