package com.megaproject.auth.repository;

import com.megaproject.auth.model.Otp;
import com.megaproject.auth.model.OtpPurpose;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpRepository extends MongoRepository<Otp, String> {

    Optional<Otp> findByUserIdAndCodeAndPurpose(String userId, String code, OtpPurpose purpose);

    void deleteAllByUserIdAndPurpose(String userId, OtpPurpose purpose);
}
