package com.megaproject.chat.config;

import com.megaproject.chat.model.ChatMessage;
import com.megaproject.chat.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMongoEventListener extends AbstractMongoEventListener<ChatMessage> {

    private final EncryptionUtil encryptionUtil;

    @Override
    public void onBeforeConvert(BeforeConvertEvent<ChatMessage> event) {
        ChatMessage message = event.getSource();
        if (message.getContent() != null) {
            message.setContent(encryptionUtil.encrypt(message.getContent()));
        }
    }

    @Override
    public void onAfterSave(AfterSaveEvent<ChatMessage> event) {
        // Restore to plaintext after saving so the returned object in memory 
        // remains usable for WebSocket broadcasting
        ChatMessage message = event.getSource();
        if (message.getContent() != null) {
            message.setContent(encryptionUtil.decrypt(message.getContent()));
        }
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<ChatMessage> event) {
        // Decrypt when reading from the database
        ChatMessage message = event.getSource();
        if (message.getContent() != null) {
            message.setContent(encryptionUtil.decrypt(message.getContent()));
        }
    }
}
