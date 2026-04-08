package com.sayedhesham.travelorch.apigateway_service.config;

import com.sayedhesham.travelorch.common.util.jwt.JwtConstants;
import com.sayedhesham.travelorch.common.util.jwt.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String X_USER_NAME = "X-User-Name";
    private static final String X_USER_ROLES = "X-User-Roles";

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        String authHeader = request.getHeaders().getFirst(JwtConstants.AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(JwtConstants.BEARER_PREFIX.length());

        try {
            if (jwtUtil.isTokenExpired(token)) {
                log.warn("Expired JWT token for path: {}", path);
                return respondWithUnauthorized(exchange, "Token has expired");
            }

            String username = jwtUtil.extractUsername(token);
            Set<String> roles = jwtUtil.extractRoles(token);

            String rolesHeader = roles.stream()
                    .sorted()
                    .collect(Collectors.joining(","));

            log.debug("Authenticated user: {} with roles: {} for path: {}", username, rolesHeader, path);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(X_USER_NAME, username)
                    .header(X_USER_ROLES, rolesHeader)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("JWT validation failed for path: {} - {}", path, e.getMessage());
            return respondWithUnauthorized(exchange, "Invalid token");
        }
    }

    private Mono<Void> respondWithUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = "{\"message\":\"" + message + "\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
