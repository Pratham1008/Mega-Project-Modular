package com.megaproject.donation.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "donations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Donation {

    @Id
    private String id;

    @Indexed
    private String donorUserId;
    private String donorName;
    private String donorEmail;

    /** In INR (paisa stored as double for simplicity) */
    private Double amount;

    /**
     * Purpose of donation — one of:
     * SCHOLARSHIP | EVENT | INFRASTRUCTURE | LIBRARY | SPORTS | OTHER
     */
    private String purpose;

    private String message;

    /** Mock payment reference — UUID generated server-side */
    private String paymentRef;

    @Builder.Default
    private String status = "SUCCESS";   // always SUCCESS for mock

    @CreatedDate
    private Instant paidAt;
}
