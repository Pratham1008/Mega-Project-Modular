package com.megaproject.chat.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Slf4j
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final String PREFIX = "[ENC]";

    @Value("${app.security.chat.encryption-key:}")
    private String encryptionKeyString;

    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        if (encryptionKeyString == null || encryptionKeyString.isBlank()) {
            throw new IllegalStateException("app.security.chat.encryption-key is required and must be configured in application.properties / environment variables!");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyString.trim());
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                throw new IllegalArgumentException("AES encryption key size must be 16, 24, or 32 bytes! Actual size: " + keyBytes.length);
            }
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
            log.info("🔐 Chat encryption utility initialized successfully with a {}-bit key.", keyBytes.length * 8);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Failed to Base64-decode chat encryption key: " + e.getMessage(), e);
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty() || plainText.startsWith(PREFIX)) {
            return plainText; 
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return PREFIX + Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Error encrypting chat message", e);
            throw new RuntimeException("Failed to encrypt message", e); 
        }
    }

    public String decrypt(String cipherTextWithPrefix) {
        if (cipherTextWithPrefix == null || !cipherTextWithPrefix.startsWith(PREFIX)) {
            
            return cipherTextWithPrefix;
        }

        try {
            String base64Cipher = cipherTextWithPrefix.substring(PREFIX.length());
            byte[] cipherMessage = Base64.getDecoder().decode(base64Cipher);

            ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting chat message", e);
            return "[Encrypted Message - Error Decrypting]";
        }
    }
}
