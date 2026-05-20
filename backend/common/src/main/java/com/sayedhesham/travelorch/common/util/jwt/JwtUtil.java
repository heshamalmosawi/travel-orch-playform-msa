package com.sayedhesham.travelorch.common.util.jwt;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import com.sayedhesham.travelorch.common.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtUtil {

    private final Key signingKey;
    private final JwtParser parser;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtUtil(String secretKey, JwtProperties jwtProperties) {
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser()
                .setSigningKey(signingKey)
                .build();
        this.accessTokenExpirationMs = jwtProperties.getAccessTokenExpirationMs();
        this.refreshTokenExpirationMs = jwtProperties.getRefreshTokenExpirationMs();
    }

    public JwtUtil(String secretKey) {
        this(secretKey, new JwtProperties());
    }

    public String generateToken(String username, String role, boolean isService) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim(JwtConstants.ROLE_CLAIM, role)
                .claim(JwtConstants.SERVICE_CLAIM, isService)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim(JwtConstants.TOKEN_TYPE_CLAIM, JwtConstants.REFRESH_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        Object roleClaim = getClaims(token).get(JwtConstants.ROLE_CLAIM);
        return roleClaim instanceof String ? (String) roleClaim : null;
    }

    public boolean isService(String token) {
        Object serviceClaim = getClaims(token).get(JwtConstants.SERVICE_CLAIM);
        if (serviceClaim instanceof Boolean aBoolean) {
            return aBoolean;
        }
        return false;
    }

    public Date extractExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Object tokenType = getClaims(token).get(JwtConstants.TOKEN_TYPE_CLAIM);
            return JwtConstants.REFRESH_TOKEN_TYPE.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateToken(String token, String username) {
        try {
            Claims claims = getClaims(token);
            String extractedUsername = claims.getSubject();
            Date expiration = claims.getExpiration();
            return extractedUsername.equals(username) &&
                    (expiration == null || !expiration.before(new Date()));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            Date expiration = claims.getExpiration();
            return expiration == null || !expiration.before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }

    private Claims getClaims(String token) {
        return parseToken(token).getPayload();
    }

    private Jws<Claims> parseToken(String token) {
        return parser.parseSignedClaims(token);
    }
}
