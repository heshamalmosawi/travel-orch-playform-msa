package com.sayedhesham.travelorch.common.config;

import com.sayedhesham.travelorch.common.util.jwt.JwtUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Configuration class for JWT authentication.
 * 
 * The JWT secret key is sourced from HashiCorp Vault via Spring Cloud Vault.
 * In production environments, the application will fail to start if the secret
 * key is not properly configured.
 * 
 * Vault path: secret/travel-system/jwt
 * Expected key in Vault: "secret"
 * Maps to property: jwt.secret-key
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private static final Logger logger = LoggerFactory.getLogger(JwtConfig.class);
    private static final int MIN_SECRET_KEY_LENGTH = 64; // 512 bits for HS512

    @Bean
    public JwtUtil jwtUtil(JwtProperties jwtProperties, Environment environment) {
        String secretKey = jwtProperties.getSecretKey();
        
        validateSecretKey(secretKey, environment);
        
        logger.info("JwtUtil initialized with Vault-sourced secret key");
        return new JwtUtil(secretKey, jwtProperties);
    }

    private void validateSecretKey(String secretKey, Environment environment) {
        if (!StringUtils.hasText(secretKey)) {
            String errorMsg = "JWT secret key is not configured. " +
                    "Ensure HashiCorp Vault is properly configured and the secret is stored at 'secret/travel-system/jwt'. " +
                    "Set VAULT_ENABLED=true and provide VAULT_TOKEN to enable Vault integration.";
            
            if (isProductionProfile(environment)) {
                throw new IllegalStateException(errorMsg);
            } else {
                logger.warn("{}. Using a development fallback is NOT recommended.", errorMsg);
                throw new IllegalStateException(errorMsg + 
                        " For local development, set jwt.secret-key in application-local.yml or environment variable JWT_SECRET_KEY.");
            }
        }

        if (secretKey.length() < MIN_SECRET_KEY_LENGTH) {
            String errorMsg = String.format(
                    "JWT secret key must be at least %d characters (512 bits) for HS512 algorithm. Current length: %d",
                    MIN_SECRET_KEY_LENGTH, secretKey.length());
            throw new IllegalStateException(errorMsg);
        }

        logger.debug("JWT secret key validation passed (length: {} characters)", secretKey.length());
    }

    private boolean isProductionProfile(Environment environment) {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}
