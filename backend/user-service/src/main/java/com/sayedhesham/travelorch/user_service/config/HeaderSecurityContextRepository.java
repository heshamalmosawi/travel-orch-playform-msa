package com.sayedhesham.travelorch.user_service.config;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HeaderSecurityContextRepository implements ServerSecurityContextRepository {

    private static final String X_USER_NAME = "X-User-Name";
    private static final String X_USER_ROLES = "X-User-Roles";

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        String username = headers.getFirst(X_USER_NAME);
        String rolesHeader = headers.getFirst(X_USER_ROLES);

        if (username == null || username.isEmpty()) {
            return Mono.empty();
        }

        List<SimpleGrantedAuthority> authorities = parseAuthorities(rolesHeader);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username, null, authorities
        );

        return Mono.just(new SecurityContextImpl(authentication));
    }

    private List<SimpleGrantedAuthority> parseAuthorities(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isEmpty()) {
            return List.of();
        }

        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }
}
