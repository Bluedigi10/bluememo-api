package com.bluedigi.bluememo.shared.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bluedigi.bluememo.config.properties.JwtProperties;
import com.bluedigi.bluememo.identity.domain.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    private static final String JWT_SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
    private static final long JWT_EXPIRATION_MS = 3_600_000L;
    private static final String NAME = "David";
    private static final String EMAIL = "david@example.com";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(
                JWT_SECRET,
                JWT_EXPIRATION_MS
        );
        jwtService = new JwtService(jwtProperties);
    }

    @Test
    void generateTokenSuccessful() {
        
        UUID userId = UUID.randomUUID();
        User savedUser = new User();
        savedUser.setId(userId);
        savedUser.setEmail(EMAIL);
        savedUser.setName(NAME);

        String token = jwtService.generateToken(savedUser);

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertAll(
                () -> assertNotNull(token),
                () -> assertEquals(userId.toString(), claims.getSubject()),
                () -> assertEquals("David", claims.get("name", String.class)),
                () -> assertEquals(
                        "david@example.com",
                        claims.get("email", String.class)
                ),
                () -> assertNotNull(claims.getIssuedAt()),
                () -> assertNotNull(claims.getExpiration()),
                () -> assertEquals(
                        JWT_EXPIRATION_MS,
                        claims.getExpiration().getTime()
                                - claims.getIssuedAt().getTime()
                ));

    }

    @Test
    void getSubjectSuccessful() {

        UUID userId = UUID.randomUUID();
        User savedUser = new User();
        savedUser.setId(userId);
        savedUser.setEmail(EMAIL);
        savedUser.setName(NAME);

        String token = jwtService.generateToken(savedUser);
        String subject = jwtService.getSubject(token);

        assertEquals(userId.toString(), subject);
    }

    @Test
    void isTokenValidSuccessful() {
        UUID userId = UUID.randomUUID();
        User savedUser = new User();
        savedUser.setId(userId);
        savedUser.setEmail(EMAIL);
        savedUser.setName(NAME);

        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(userId.toString())
                .password("12345678")
                .roles("USER")
                .build();

        String token = jwtService.generateToken(savedUser);

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    void isTokenInvalidNull() {
        UUID userId = UUID.randomUUID();
        User savedUser = new User();
        savedUser.setId(userId);
        savedUser.setEmail(EMAIL);
        savedUser.setName(NAME);

        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(userId.toString())
                .password("12345678")
                .roles("USER")
                .build();

        boolean isValid = jwtService.isTokenValid(null, userDetails);

        assertFalse(isValid);
    }

    @Test
    void isTokenInvalidCorrupted() {
        UUID userId = UUID.randomUUID();
        User savedUser = new User();
        savedUser.setId(userId);
        savedUser.setEmail(EMAIL);
        savedUser.setName(NAME);

        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(userId.toString())
                .password("12345678")
                .roles("USER")
                .build();

        String token = jwtService.generateToken(savedUser);

        // Corrupt the token
        token = token.substring(0, token.length() - 1) + "invalid";

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertFalse(isValid);
    }

    @Test
    void isTokenInvalidBadUsername() {
        UUID userId = UUID.randomUUID();
        User savedUser = new User();
        savedUser.setId(userId);
        savedUser.setEmail(EMAIL);
        savedUser.setName(NAME);

        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(userId + "bad")
                .password("12345678")
                .roles("USER")
                .build();

        String token = jwtService.generateToken(savedUser);

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertFalse(isValid);
    }

    @Test
    void isTokenInvalidExpired() {
        UUID userId = UUID.randomUUID();
        User savedUser = new User();
        savedUser.setId(userId);
        savedUser.setEmail(EMAIL);
        savedUser.setName(NAME);
        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(userId.toString())
                .password("12345678")
                .roles("USER")
                .build();
        // Set expiration to 1 second for testing
        JwtProperties expiredProperties = new JwtProperties(
            JWT_SECRET,
            1L
        );

        JwtService expiredJwtService = new JwtService(expiredProperties);
        String token = expiredJwtService.generateToken(savedUser);
        // Wait for the token to expire
        try {
                Thread.sleep(15L);
        } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
        }
        boolean isValid = expiredJwtService.isTokenValid(token, userDetails);
        assertFalse(isValid);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(JWT_SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
