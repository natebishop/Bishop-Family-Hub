package com.familyhub.demo.service;

import com.familyhub.demo.config.JwtConfig;
import com.familyhub.demo.model.Family;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Date;

import static com.familyhub.demo.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtConfig jwtConfig;
    private Family family;

    // 256-bit key encoded as base64 for HMAC-SHA (exactly 32 bytes)
    private static final String TEST_SECRET = Base64.getEncoder()
            .encodeToString("this-is-a-test-secret-key-32-by!".getBytes());
    private static final long EXPIRATION = 3600000; // 1 hour
    private static final String ISSUER = "test-issuer";

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret(TEST_SECRET);
        jwtConfig.setExpiration(EXPIRATION);
        jwtConfig.setIssuer(ISSUER);

        jwtService = new JwtService(jwtConfig);

        family = createFamily();
    }

    @Test
    void generateToken_containsCorrectSubject() {
        String token = jwtService.generateToken(family);

        String subject = jwtService.extractSubject(token);
        assertThat(subject).isEqualTo(FAMILY_ID.toString());
    }

    @Test
    void extractSubject_validToken_returnsId() {
        String token = jwtService.generateToken(family);

        String subject = jwtService.extractSubject(token);

        assertThat(subject).isEqualTo(family.getId().toString());
    }

    @Test
    void extractSubject_expiredToken_throwsJwtException() {
        // Build an expired token manually
        JwtConfig expiredConfig = new JwtConfig();
        expiredConfig.setSecret(TEST_SECRET);
        expiredConfig.setExpiration(0);
        expiredConfig.setIssuer(ISSUER);

        String expiredToken = Jwts.builder()
                .subject(family.getId().toString())
                .issuer(ISSUER)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .compact();

        assertThatThrownBy(() -> jwtService.extractSubject(expiredToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractSubject_tamperedToken_throws() {
        String token = jwtService.generateToken(family);
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtService.extractSubject(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractSubject_wrongIssuer_throws() {
        // Build a token with a different issuer
        String wrongIssuerToken = Jwts.builder()
                .subject(family.getId().toString())
                .issuer("wrong-issuer")
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .compact();

        assertThatThrownBy(() -> jwtService.extractSubject(wrongIssuerToken))
                .isInstanceOf(JwtException.class);
    }
}
