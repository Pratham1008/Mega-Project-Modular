package com.megaproject.chat.dto;

import lombok.Data;
import java.util.List;


@Data
public class ConversationRequest {
    
    private List<String> participantIds;
    private String title;       
    private boolean group;
}
