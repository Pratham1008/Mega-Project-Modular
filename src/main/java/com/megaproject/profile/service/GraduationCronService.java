package com.megaproject.profile.service;

import com.megaproject.auth.model.Role;
import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.model.ProfileType;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraduationCronService {

    private final ProfileRepository profileRepository;
    private final MongoTemplate mongoTemplate;

    @Scheduled(cron = "0 0 0 1 8 *")
    public void promoteStudentsToAlumni() {
        int currentYear = Year.now().getValue();

        List<ProfileDocument> eligibleStudents = profileRepository
                .findByProfileTypeAndDeletedFalseAndPassingYearLessThan(ProfileType.STUDENT, currentYear);

        if (eligibleStudents.isEmpty()) {
            log.info("No eligible students to promote to ALUMNI.");
            return;
        }

        
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ProfileDocument.class);
        List<String> userIds = new ArrayList<>();

        for (ProfileDocument student : eligibleStudents) {
            Query query = new Query(Criteria.where("_id").is(student.getId()));
            Update update = new Update().set("profileType", ProfileType.ALUMNI);
            bulkOps.updateOne(query, update);
            userIds.add(student.getUserId());
        }

        try {
            bulkOps.execute();
            log.info("Bulk updated {} profiles to ALUMNI.", eligibleStudents.size());
        } catch (Exception e) {
            log.error("Bulk profile update failed", e);
        }

        
        try {
            BulkOperations userBulkOps = mongoTemplate.bulkOps(
                    BulkOperations.BulkMode.UNORDERED, "users");
            for (String userId : userIds) {
                Query q = new Query(Criteria.where("_id").is(userId));
                Update u = new Update().set("role", Role.ALUMNI);
                userBulkOps.updateOne(q, u);
            }
            var result = userBulkOps.execute();
            log.info("Graduation promotion complete: {} user roles updated to ALUMNI out of {} eligible.",
                    result.getModifiedCount(), eligibleStudents.size());
        } catch (Exception e) {
            log.error("Bulk user role update failed for {} users", userIds.size(), e);
        }
    }
}
