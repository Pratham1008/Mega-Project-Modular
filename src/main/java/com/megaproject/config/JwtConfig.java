package com.megaproject.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@Configuration
@Getter
@Slf4j
public class JwtConfig {

    private final long accessTokenExpirySeconds = 3600 * 24L;       
    private final long refreshTokenExpirySeconds = 3600 * 24 * 7L;  

    @Value("${app.jwt.rsa-public-key:}")
    private String rsaPublicKeyPem;

    @Value("${app.jwt.rsa-private-key:}")
    private String rsaPrivateKeyPem;

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private String keyId;

    @PostConstruct
    public void initKeys() throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (rsaPublicKeyPem != null && !rsaPublicKeyPem.isBlank()
                && rsaPrivateKeyPem != null && !rsaPrivateKeyPem.isBlank()) {
            // Load keys from environment variables
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            byte[] publicKeyBytes = Base64.getDecoder().decode(stripPemHeaders(rsaPublicKeyPem));
            this.publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            byte[] privateKeyBytes = Base64.getDecoder().decode(stripPemHeaders(rsaPrivateKeyPem));
            this.privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            this.keyId = UUID.randomUUID().toString();
            log.info("RSA keypair loaded from environment variables, kid = {}", keyId);
        } else {
            // Dev mode: auto-generate keys
            log.warn("JWT_RSA_PUBLIC_KEY / JWT_RSA_PRIVATE_KEY not set — generating ephemeral RSA keypair (dev mode). "
                    + "Tokens will NOT survive restarts!");
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            this.privateKey = (RSAPrivateKey) pair.getPrivate();
            this.publicKey  = (RSAPublicKey)  pair.getPublic();
            this.keyId      = UUID.randomUUID().toString();
            log.info("RSA keypair generated with kid = {}", keyId);
        }
    }

    /**
     * Strips PEM header/footer lines and whitespace so only the Base64 payload remains.
     */
    private String stripPemHeaders(String pem) {
        return pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
    }
}
