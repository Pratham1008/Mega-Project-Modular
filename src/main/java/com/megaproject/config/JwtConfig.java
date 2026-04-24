package com.megaproject.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
@Getter
public class JwtConfig {

    private final long accessTokenExpirySeconds = 3600 * 24L;       // 24 hours
    private final long refreshTokenExpirySeconds = 3600 * 24 * 7L;  // 7 days

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private String keyId;

    @PostConstruct
    public void initKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        this.privateKey = (RSAPrivateKey) pair.getPrivate();
        this.publicKey  = (RSAPublicKey)  pair.getPublic();
        this.keyId      = UUID.randomUUID().toString();
        System.out.println("🔐 RSA keypair generated with kid = " + keyId);
    }
}
