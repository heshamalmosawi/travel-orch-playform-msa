package com.sayedhesham.travelorch.user_service.service;

import com.sayedhesham.travelorch.common.entity.rbac.Role;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.common.repository.rbac.RoleRepository;
import com.sayedhesham.travelorch.common.util.jwt.JwtUtil;
import com.sayedhesham.travelorch.user_service.dto.AuthResponse;
import com.sayedhesham.travelorch.user_service.dto.LoginRequest;
import com.sayedhesham.travelorch.user_service.dto.RegistrationRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TransactionTemplate transactionTemplate;

    public Mono<AuthResponse> register(RegistrationRequest request) {
        logger.info("Registering user: {}, email: {}", request.getUsername(), request.getEmail());
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            logger.debug("Checking if username exists: {}", request.getUsername());
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new IllegalArgumentException("Username already exists");
            }

            logger.debug("Checking if email exists: {}", request.getEmail());
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists");
            }

            logger.debug("Creating user and generating token");
            return createUserAndGenerateToken(request);
        })).subscribeOn(Schedulers.boundedElastic())
          .doOnError(e -> logger.error("Error during registration for user {}: {}", request.getUsername(), e.getMessage(), e));
    }

    public Mono<AuthResponse> login(LoginRequest request) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User user = findUserByUsernameOrEmail(request.getUsername());

            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("Invalid credentials");
            }

            return generateAuthResponse(user, user.getRoles(), "Login successful");
        })).subscribeOn(Schedulers.boundedElastic());
    }

    private User findUserByUsernameOrEmail(String username) {
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
    }

    private AuthResponse createUserAndGenerateToken(RegistrationRequest request) {
        logger.debug("Fetching default roles");
        Set<Role> roles = getDefaultRoles();
        logger.debug("Default roles fetched: {}", roles.stream().map(Role::getName).collect(Collectors.toSet()));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .roles(roles)
                .build();

        logger.debug("Saving user to database");
        User savedUser = userRepository.save(user);
        logger.debug("User saved with id: {}", savedUser.getId());
        return generateAuthResponse(savedUser, roles, "User registered successfully");
    }

    private Set<Role> getDefaultRoles() {
        logger.debug("Looking up 'user' role");
        Role userRole = roleRepository.findByName("user")
                .orElseThrow(() -> new IllegalStateException("Default user role not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        return roles;
    }

    private AuthResponse generateAuthResponse(User user, Set<Role> roles, String message) {
        Set<String> roleNames = roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        logger.debug("Generating JWT token for user: {}", user.getUsername());
        String token = jwtUtil.generateToken(user.getUsername(), roleNames, false);
        logger.debug("Token generated successfully");

        return AuthResponse.builder()
                .message(message)
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .build();
    }
}
