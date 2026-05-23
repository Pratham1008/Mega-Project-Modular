package com.megaproject.notification.repository;

import com.megaproject.notification.model.FcmToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends MongoRepository<FcmToken, String> {
    List<FcmToken> findByUserId(String userId);
    Optional<FcmToken> findByToken(String token);
    void deleteByToken(String token);
    void deleteByUserIdAndToken(String userId, String token);

    void deleteAllByTokenIn(List<String> toDelete);
}
