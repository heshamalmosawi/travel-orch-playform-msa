package com.sayedhesham.travelorch.payment_service.security;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
            Set<String> roles = extractRoles(claims);

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    username, null, authorities
            );

            log.info("load [{}] - Authenticated user: {} with roles: {}", path, username, roles);
            return Mono.just(new SecurityContextImpl(authentication));
        } catch (Exception e) {
            log.warn("load [{}] - JWT parsing failed: {}", path, e.getMessage());
            return Mono.empty();
        }
    }

    private Set<String> extractRoles(Claims claims) {
        Object rolesClaim = claims.get(JwtConstants.ROLES_CLAIM);
        if (rolesClaim instanceof List) {
            return ((List<?>) rolesClaim).stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }
}
