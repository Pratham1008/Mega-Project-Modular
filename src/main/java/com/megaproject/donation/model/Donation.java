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

    
    private Double amount;

    
    private String purpose;

    private String message;

    
    private String paymentRef;

    @Builder.Default
    private String status = "SUCCESS";   

    @CreatedDate
    private Instant paidAt;
}
