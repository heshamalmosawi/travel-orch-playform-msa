package com.sayedhesham.travelorch.user_service.security;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;

import com.sayedhesham.travelorch.common.util.jwt.JwtConstants;
import com.sayedhesham.travelorch.common.util.jwt.JwtUtil;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

public class JwtSecurityContextRepository implements ServerSecurityContextRepository {

    private static final Logger log = LoggerFactory.getLogger(JwtSecurityContextRepository.class);

    private final JwtUtil jwtUtil;

    public JwtSecurityContextRepository(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
            log.debug("load [{}] - No Bearer token found", path);
            return Mono.empty();
        }

        String token = authHeader.substring(JwtConstants.BEARER_PREFIX.length());

        try {
            Claims claims = jwtUtil.parseClaims(token);

            if (claims.getExpiration() != null && claims.getExpiration().before(new Date())) {
                log.warn("load [{}] - JWT expired at {}", path, claims.getExpiration());
                return Mono.empty();
            }

            String username = claims.getSubject();
            String role = jwtUtil.extractRole(token);

            List<SimpleGrantedAuthority> authorities = List.of();
            if (role != null) {
                authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    username, null, authorities
            );

            log.info("load [{}] - Authenticated user: {} with role: {}", path, username, role);
            return Mono.just(new SecurityContextImpl(authentication));
        } catch (Exception e) {
            log.warn("load [{}] - JWT parsing failed: {}", path, e.getMessage());
            return Mono.empty();
        }
    }
}