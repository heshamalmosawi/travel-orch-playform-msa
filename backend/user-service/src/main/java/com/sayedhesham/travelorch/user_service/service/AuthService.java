package com.sayedhesham.travelorch.user_service.service;

import com.sayedhesham.travelorch.common.entity.rbac.Role;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.common.repository.rbac.RoleRepository;
import com.sayedhesham.travelorch.common.util.jwt.JwtUtil;
import com.sayedhesham.travelorch.user_service.dto.AuthResponse;
import com.sayedhesham.travelorch.user_service.dto.RegistrationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegistrationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        return createUserAndGenerateToken(request);
    }

    private AuthResponse createUserAndGenerateToken(RegistrationRequest request) {
        Set<Role> roles = getDefaultRoles();

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        return generateAuthResponse(savedUser, roles);
    }

    private Set<Role> getDefaultRoles() {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Default USER role not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        return roles;
    }

    private AuthResponse generateAuthResponse(User user, Set<Role> roles) {
        Set<String> roleNames = roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String token = jwtUtil.generateToken(user.getUsername(), roleNames, false);

        return AuthResponse.builder()
                .message("User registered successfully")
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .build();
    }
}
