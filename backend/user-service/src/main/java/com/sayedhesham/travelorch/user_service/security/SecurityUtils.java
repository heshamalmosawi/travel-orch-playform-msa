package com.sayedhesham.travelorch.user_service.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Mono<String> getCurrentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth.isAuthenticated()
                        && !(auth instanceof AnonymousAuthenticationToken))
                .map(Authentication::getName);
    }
}
