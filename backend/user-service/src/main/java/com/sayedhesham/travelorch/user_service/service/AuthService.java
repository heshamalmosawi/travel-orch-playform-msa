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

            return generateAuthResponse(user, user.getRole(), "Login successful");
        })).subscribeOn(Schedulers.boundedElastic());
    }

    private User findUserByUsernameOrEmail(String username) {
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
    }

    private AuthResponse createUserAndGenerateToken(RegistrationRequest request) {
        logger.debug("Fetching roles for registration");
        Role role = getRoleForRegistration(request.getRole());
        logger.debug("Role fetched: {}", role.getName());

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(role)
                .build();

        logger.debug("Saving user to database");
        User savedUser = userRepository.save(user);
        logger.debug("User saved with id: {}", savedUser.getId());
        return generateAuthResponse(savedUser, role, "User registered successfully");
    }

    private Role getRoleForRegistration(String requestedRole) {
        String roleName = requestedRole != null && !requestedRole.isBlank()
                ? requestedRole.trim().toLowerCase()
                : "user";

        if (!roleName.equals("user") && !roleName.equals("travel_manager")) {
            throw new IllegalArgumentException("Invalid role: " + roleName + ". Only 'user' and 'travel_manager' are allowed for self-registration.");
        }

        logger.debug("Looking up role: {}", roleName);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));

        return role;
    }

    private AuthResponse generateAuthResponse(User user, Role role, String message) {
        logger.debug("Generating JWT token for user: {}", user.getUsername());
        String token = jwtUtil.generateToken(user.getUsername(), role.getName(), false);
        logger.debug("Token generated successfully");

        return AuthResponse.builder()
                .message(message)
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .build();
    }
}
