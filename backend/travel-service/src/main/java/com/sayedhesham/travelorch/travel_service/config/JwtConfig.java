package com.sayedhesham.travelorch.travel_service.config;

import com.sayedhesham.travelorch.common.config.JwtProperties;
import com.sayedhesham.travelorch.common.util.jwt.JwtUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public JwtUtil jwtUtil(JwtProperties jwtProperties) {
        String secretKey = jwtProperties.getSecretKey();
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException("JWT secret key is not configured. Please set jwt.secret-key in configuration.");
        }
        return new JwtUtil(secretKey, jwtProperties);
    }
}
