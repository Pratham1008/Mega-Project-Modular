package com.megaproject.donation.repository;

import com.megaproject.donation.model.Donation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DonationRepository extends MongoRepository<Donation, String> {
    List<Donation> findByDonorUserIdOrderByPaidAtDesc(String donorUserId);
    List<Donation> findAllByOrderByPaidAtDesc();
}
