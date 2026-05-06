package com.megaproject.donation.controller;

import com.megaproject.donation.dto.DonationRequest;
import com.megaproject.donation.model.Donation;
import com.megaproject.donation.repository.DonationRepository;
import com.megaproject.profile.repository.ProfileRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationRepository donationRepo;
    private final ProfileRepository profileRepo;

    @PostMapping
    @PreAuthorize("hasRole('ALUMNI')")
    public ResponseEntity<Donation> donate(
            @Valid @RequestBody DonationRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        String donorName = profileRepo.findByUserId(userId)
                .map(p -> p.getFullName())
                .orElse("Anonymous");

        Donation donation = Donation.builder()
                .donorUserId(userId)
                .donorName(donorName)
                .donorEmail(jwt.getClaimAsString("email"))
                .amount(req.getAmount())
                .purpose(req.getPurpose())
                .message(req.getMessage())
                .paymentRef("KIT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status("SUCCESS")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(donationRepo.save(donation));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listAll() {
        List<Donation> all = donationRepo.findAllByOrderByPaidAtDesc();
        double total = all.stream().mapToDouble(Donation::getAmount).sum();
        return ResponseEntity.ok(Map.of(
                "total", total,
                "count", all.size(),
                "recent", all.size() > 10 ? all.subList(0, 10) : all
        ));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Donation>> myDonations(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(donationRepo.findByDonorUserIdOrderByPaidAtDesc(jwt.getSubject()));
    }
}
