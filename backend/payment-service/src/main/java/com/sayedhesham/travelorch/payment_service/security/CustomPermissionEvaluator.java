package com.sayedhesham.travelorch.payment_service.security;

import java.io.Serializable;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;

public class CustomPermissionEvaluator implements PermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(CustomPermissionEvaluator.class);

    private final UserRepository userRepository;

    public CustomPermissionEvaluator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        String resource = (String) targetDomainObject;
        String action = (String) permission;
        return checkPermission(authentication, resource, action);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String resource, Object action) {
        return checkPermission(authentication, resource, (String) action);
    }

    private boolean checkPermission(Authentication authentication, String resource, String action) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("checkPermission - No authenticated user for resource: {}, action: {}", resource, action);
            return false;
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            log.warn("checkPermission - User {} not found in DB for resource: {}, action: {}", username, resource, action);
            return false;
        }

        boolean granted = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(permission ->
                        resource.equalsIgnoreCase(permission.getResource()) &&
                        action.equalsIgnoreCase(permission.getAction())
                );

        if (granted) {
            log.info("checkPermission - GRANTED user: {} resource: {} action: {} (roles: {})", username, resource, action,
                    user.getRoles().stream().map(r -> r.getName()).collect(Collectors.joining(",")));
        } else {
            log.warn("checkPermission - DENIED user: {} resource: {} action: {} (roles: {})", username, resource, action,
                    user.getRoles().stream().map(r -> r.getName()).collect(Collectors.joining(",")));
        }

        return granted;
    }
}
