package com.megaproject.chat.dto;

import lombok.Data;
import java.util.List;

/** Request to create a group conversation or get/create a DM */
@Data
public class ConversationRequest {
    /** For DM: just one other userId. For group: multiple userIds + title */
    private List<String> participantIds;
    private String title;       // only for groups
    private boolean group;
}
