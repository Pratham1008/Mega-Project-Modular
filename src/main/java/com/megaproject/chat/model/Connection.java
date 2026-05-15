package com.megaproject.chat.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;


@Document(collection = "connections")
@CompoundIndex(name = "requester_receiver", def = "{'requesterId': 1, 'receiverId': 1}", unique = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Connection {

    @Id
    private String id;

    
    private String requesterId;
    private String requesterName;
    private String requesterPhotoUrl;

    
    private String receiverId;
    private String receiverName;
    private String receiverPhotoUrl;

    @Builder.Default
    private ConnectionStatus status = ConnectionStatus.PENDING;

    @CreatedDate
    private Instant createdAt;

    private Instant respondedAt;

    public enum ConnectionStatus {
        PENDING, ACCEPTED, REJECTED
    }
}
