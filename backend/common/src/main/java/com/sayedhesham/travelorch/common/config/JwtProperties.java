package com.sayedhesham.travelorch.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT signing secret key. Must be at least 256 bits (32 characters) for HS512.
     * This value should be injected from HashiCorp Vault via Spring Cloud Vault.
     * In production, this MUST be set - the application will fail to start without it.
     */
    private String secretKey;

    private long accessTokenExpirationMs = 15 * 60 * 1000; // 15 minutes
    private long refreshTokenExpirationMs = 7 * 24 * 60 * 60 * 1000; // 7 days

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public void setAccessTokenExpirationMs(long accessTokenExpirationMs) {
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public void setRefreshTokenExpirationMs(long refreshTokenExpirationMs) {
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }
}
