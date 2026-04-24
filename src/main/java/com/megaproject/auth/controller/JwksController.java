package com.megaproject.auth.controller;

import com.megaproject.config.JwtConfig;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwtConfig jwtConfig;

    @GetMapping("/auth/.well-known/jwks.json")
    public Map<String, Object> getJwks() {
        RSAKey rsaKey = new RSAKey.Builder(jwtConfig.getPublicKey())
                .keyID(jwtConfig.getKeyId())
                .build();
        return new JWKSet(rsaKey).toJSONObject();
    }
}
