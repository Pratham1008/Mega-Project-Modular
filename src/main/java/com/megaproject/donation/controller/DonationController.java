package com.megaproject.donation.controller;

import com.megaproject.donation.dto.DonationRequest;
import com.megaproject.donation.dto.OrderResponse;
import com.megaproject.donation.dto.PaymentVerifyRequest;
import com.megaproject.donation.model.Donation;
import com.megaproject.donation.repository.DonationRepository;
import com.megaproject.donation.service.RazorpayService;
import com.megaproject.profile.repository.ProfileRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/donations")
@RequiredArgsConstructor
@Slf4j
public class DonationController {

    private final DonationRepository donationRepo;
    private final ProfileRepository profileRepo;
    private final RazorpayService razorpayService;
    private final com.megaproject.donation.service.DonationCampaignService campaignService;

    @Value("${razorpay.key.id}")
    private String keyId;

    @PostMapping("/campaign/trigger")
    public ResponseEntity<Map<String, String>> triggerCampaign(
            @RequestParam(defaultValue = "1") int phase,
            @AuthenticationPrincipal Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only admins can trigger campaigns"));
        }
        campaignService.runCampaignForPhase(phase);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Campaign phase " + phase + " triggered"));
    }

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('ALUMNI')")
    public ResponseEntity<?> createOrder(@Valid @RequestBody DonationRequest req) {
        try {
            Order order = razorpayService.createOrder(Math.round(req.getAmount() * 100));
            OrderResponse response = OrderResponse.builder()
                    .orderId(order.get("id"))
                    .amount(req.getAmount())
                    .currency("INR")
                    .keyId(keyId)
                    .build();
            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create Razorpay order"));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ALUMNI')")
    public ResponseEntity<?> donate(
            @Valid @RequestBody PaymentVerifyRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        boolean isValid = razorpayService.verifySignature(
                req.getRazorpayOrderId(),
                req.getRazorpayPaymentId(),
                req.getRazorpaySignature()
        );

        if (!isValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Payment signature verification failed"));
        }

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
                .paymentRef(req.getRazorpayPaymentId())
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
