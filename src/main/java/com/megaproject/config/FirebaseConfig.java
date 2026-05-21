package com.megaproject.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Base64;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.service-key:}")
    private String firebaseServiceKey;

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = null;

                if (firebaseServiceKey != null && !firebaseServiceKey.isBlank()) {
                    byte[] decoded = Base64.getDecoder().decode(firebaseServiceKey);
                    serviceAccount = new ByteArrayInputStream(decoded);
                    log.info("Using FIREBASE_SERVICE_KEY from environment variable");
                } else {
                    try {
                        serviceAccount = new FileInputStream("service_key.json");
                    } catch (Exception e) {
                        serviceAccount = getClass().getClassLoader().getResourceAsStream("service_key.json");
                    }
                }

                if (serviceAccount == null) {
                    log.warn("Firebase service key not found — FCM disabled");
                    return null;
                }
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized");
            }
            return FirebaseMessaging.getInstance();
        } catch (Exception e) {
            log.error("Failed to init Firebase: {}", e.getMessage());
            return null;
        }
    }
}
