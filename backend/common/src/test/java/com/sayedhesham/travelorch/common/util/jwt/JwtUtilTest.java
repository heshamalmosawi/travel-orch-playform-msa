package com.sayedhesham.travelorch.common.util.jwt;

import com.sayedhesham.travelorch.common.config.JwtProperties;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private static final String SECRET_KEY = "test-secret-key-must-be-at-least-256-bits-long-for-hs512-algorithm-and-even-more-secure";
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 15 * 60 * 1000;
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000;

    private JwtUtil jwtUtil;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setAccessTokenExpirationMs(ACCESS_TOKEN_EXPIRATION_MS);
        jwtProperties.setRefreshTokenExpirationMs(REFRESH_TOKEN_EXPIRATION_MS);
        jwtUtil = new JwtUtil(SECRET_KEY, jwtProperties);
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate valid access token with all claims")
        void shouldGenerateValidAccessToken() {
            String username = "testuser";
            String role = "user";
            boolean isService = false;

            String token = jwtUtil.generateToken(username, role, isService);

            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(token.split("\\.").length == 3);
        }

        @Test
        @DisplayName("Should generate valid refresh token")
        void shouldGenerateValidRefreshToken() {
            String username = "testuser";

            String refreshToken = jwtUtil.generateRefreshToken(username);

            assertNotNull(refreshToken);
            assertFalse(refreshToken.isEmpty());
            assertTrue(refreshToken.split("\\.").length == 3);
            assertTrue(jwtUtil.isRefreshToken(refreshToken));
        }

        @Test
        @DisplayName("Should generate different tokens for different roles")
        void shouldGenerateDifferentTokens() {
            String username = "testuser";

            String token1 = jwtUtil.generateToken(username, "user", false);
            String token2 = jwtUtil.generateToken(username, "admin", false);

            assertNotEquals(token1, token2);
        }
    }

    @Nested
    @DisplayName("Token Extraction Tests")
    class TokenExtractionTests {

        private String validToken;

        @BeforeEach
        void createValidToken() {
            validToken = jwtUtil.generateToken("testuser", "user", false);
        }

        @Test
        @DisplayName("Should extract username correctly")
        void shouldExtractUsername() {
            String username = jwtUtil.extractUsername(validToken);
            assertEquals("testuser", username);
        }

        @Test
        @DisplayName("Should extract role correctly")
        void shouldExtractRole() {
            String role = jwtUtil.extractRole(validToken);
            assertEquals("user", role);
        }

        @Test
        @DisplayName("Should extract expiration date correctly")
        void shouldExtractExpiration() {
            Date expiration = jwtUtil.extractExpiration(validToken);
            assertNotNull(expiration);
            assertTrue(expiration.after(new Date()));
        }

        @Test
        @DisplayName("Should identify service tokens correctly")
        void shouldIdentifyServiceTokens() {
            String serviceToken = jwtUtil.generateToken("service-user", "service", true);
            assertTrue(jwtUtil.isService(serviceToken));

            String userToken = jwtUtil.generateToken("regular-user", "user", false);
            assertFalse(jwtUtil.isService(userToken));
        }

        @Test
        @DisplayName("Should identify refresh tokens correctly")
        void shouldIdentifyRefreshTokens() {
            String refreshToken = jwtUtil.generateRefreshToken("testuser");
            assertTrue(jwtUtil.isRefreshToken(refreshToken));

            String accessToken = jwtUtil.generateToken("testuser", "user", false);
            assertFalse(jwtUtil.isRefreshToken(accessToken));
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate valid token without username check")
        void shouldValidateValidToken() {
            String token = jwtUtil.generateToken("testuser", "user", false);

            assertTrue(jwtUtil.validateToken(token));
        }

        @Test
        @DisplayName("Should validate valid token with username check")
        void shouldValidateValidTokenWithUsername() {
            String token = jwtUtil.generateToken("testuser", "user", false);

            assertTrue(jwtUtil.validateToken(token, "testuser"));
        }

        @Test
        @DisplayName("Should fail validation for wrong username")
        void shouldFailValidationForWrongUsername() {
            String token = jwtUtil.generateToken("testuser", "user", false);

            assertFalse(jwtUtil.validateToken(token, "wronguser"));
        }

        @Test
        @DisplayName("Should detect expired token")
        void shouldDetectExpiredToken() {
            // Generate a token that is already expired (expiration in the past)
            Date now = new Date();
            Date pastExpiration = new Date(now.getTime() - 1000); // Expired 1 second ago
            
            String expiredToken = io.jsonwebtoken.Jwts.builder()
                    .subject("testuser")
                    .claim("role", "user")
                    .claim("service", false)
                    .issuedAt(new Date(now.getTime() - 2000)) // Issued 2 seconds ago
                    .expiration(pastExpiration)
                    .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                    .compact();

            assertTrue(jwtUtil.isTokenExpired(expiredToken));
            assertFalse(jwtUtil.validateToken(expiredToken));
        }

        @Test
        @DisplayName("Should fail validation for malformed token")
        void shouldFailValidationForMalformedToken() {
            String malformedToken = "invalid.token.here";

            assertFalse(jwtUtil.validateToken(malformedToken));
        }

        @Test
        @DisplayName("Should fail validation for empty token")
        void shouldFailValidationForEmptyToken() {
            assertFalse(jwtUtil.validateToken(""));
            assertFalse(jwtUtil.validateToken(null));
        }

        @Test
        @DisplayName("Should fail validation for tampered token")
        void shouldFailValidationForTamperedToken() {
            String token = jwtUtil.generateToken("testuser", "user", false);

            String tamperedToken = token + "tampered";

            assertFalse(jwtUtil.validateToken(tamperedToken));
        }
    }

    @Nested
    @DisplayName("Token Expiration Tests")
    class TokenExpirationTests {

        @Test
        @DisplayName("Should detect valid token as not expired")
        void shouldDetectValidTokenAsNotExpired() {
            String token = jwtUtil.generateToken("testuser", "user", false);

            assertFalse(jwtUtil.isTokenExpired(token));
        }

        @Test
        @DisplayName("Should return correct expiration time for access token")
        void shouldReturnCorrectExpirationForAccessToken() {
            long beforeGeneration = System.currentTimeMillis();

            String token = jwtUtil.generateToken("testuser", "user", false);
            Date expiration = jwtUtil.extractExpiration(token);
            long afterGeneration = System.currentTimeMillis();

            long expectedMinExpiration = beforeGeneration + ACCESS_TOKEN_EXPIRATION_MS - 1000;
            long expectedMaxExpiration = afterGeneration + ACCESS_TOKEN_EXPIRATION_MS + 1000;

            assertTrue(expiration.getTime() >= expectedMinExpiration);
            assertTrue(expiration.getTime() <= expectedMaxExpiration);
        }

        @Test
        @DisplayName("Should return correct expiration time for refresh token")
        void shouldReturnCorrectExpirationForRefreshToken() {
            long beforeGeneration = System.currentTimeMillis();
            
            String token = jwtUtil.generateRefreshToken("testuser");
            Date expiration = jwtUtil.extractExpiration(token);
            long afterGeneration = System.currentTimeMillis();

            long expectedMinExpiration = beforeGeneration + REFRESH_TOKEN_EXPIRATION_MS - 1000;
            long expectedMaxExpiration = afterGeneration + REFRESH_TOKEN_EXPIRATION_MS + 1000;

            assertTrue(expiration.getTime() >= expectedMinExpiration);
            assertTrue(expiration.getTime() <= expectedMaxExpiration);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle single role extraction")
        void shouldHandleSingleRole() {
            String token = jwtUtil.generateToken("testuser", "admin", false);

            String extractedRole = jwtUtil.extractRole(token);
            assertEquals("admin", extractedRole);
        }

        @Test
        @DisplayName("Should handle special characters in username")
        void shouldHandleSpecialCharactersInUsername() {
            String username = "test.user+special@domain.com";
            String token = jwtUtil.generateToken(username, "user", false);

            assertEquals(username, jwtUtil.extractUsername(token));
            assertTrue(jwtUtil.validateToken(token));
        }

        @Test
        @DisplayName("Should extract role from token with any role name")
        void shouldHandleAnyRoleName() {
            String token = jwtUtil.generateToken("testuser", "travel_manager", false);

            String extractedRole = jwtUtil.extractRole(token);
            assertEquals("travel_manager", extractedRole);
        }

        @Test
        @DisplayName("Should throw exception for invalid token when extracting claims")
        void shouldThrowExceptionForInvalidToken() {
            String invalidToken = "this.is.not.a.valid.jwt";

            assertThrows(MalformedJwtException.class, () -> jwtUtil.extractUsername(invalidToken));
            assertThrows(MalformedJwtException.class, () -> jwtUtil.extractRole(invalidToken));
        }
    }

    @Nested
    @DisplayName("Constants Tests")
    class ConstantsTests {

        @Test
        @DisplayName("Should have correct authorization header name")
        void shouldHaveCorrectAuthorizationHeader() {
            assertEquals("Authorization", JwtConstants.AUTHORIZATION_HEADER);
        }

        @Test
        @DisplayName("Should have correct bearer prefix")
        void shouldHaveCorrectBearerPrefix() {
            assertEquals("Bearer ", JwtConstants.BEARER_PREFIX);
        }

        @Test
        @DisplayName("Should have correct claim names")
        void shouldHaveCorrectClaimNames() {
            assertEquals("role", JwtConstants.ROLE_CLAIM);
            assertEquals("service", JwtConstants.SERVICE_CLAIM);
            assertEquals("token_type", JwtConstants.TOKEN_TYPE_CLAIM);
        }

        @Test
        @DisplayName("Should have correct token types")
        void shouldHaveCorrectTokenTypes() {
            assertEquals("access", JwtConstants.ACCESS_TOKEN_TYPE);
            assertEquals("refresh", JwtConstants.REFRESH_TOKEN_TYPE);
        }

        @Test
        @DisplayName("Should prevent instantiation of utility class")
        void shouldPreventInstantiation() {
            assertThrows(UnsupportedOperationException.class, () -> {
                try {
                    java.lang.reflect.Constructor<JwtConstants> constructor = JwtConstants.class.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    constructor.newInstance();
                } catch (java.lang.reflect.InvocationTargetException e) {
                    if (e.getCause() instanceof UnsupportedOperationException) {
                        throw (UnsupportedOperationException) e.getCause();
                    }
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
