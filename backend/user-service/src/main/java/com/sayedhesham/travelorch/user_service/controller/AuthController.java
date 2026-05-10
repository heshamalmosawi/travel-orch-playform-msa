package com.sayedhesham.travelorch.user_service.controller;

import com.sayedhesham.travelorch.user_service.dto.AuthResponse;
import com.sayedhesham.travelorch.user_service.dto.LoginRequest;
import com.sayedhesham.travelorch.user_service.dto.RegistrationRequest;
import com.sayedhesham.travelorch.user_service.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(@Valid @RequestBody RegistrationRequest registrationRequest) {
        logger.info("Registration request received for username: {}", registrationRequest.getUsername());
        return authService.register(registrationRequest)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(IllegalArgumentException.class, e -> {
                    logger.warn("Registration failed for username {}: {}", registrationRequest.getUsername(), e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(
                            AuthResponse.builder()
                                    .message(e.getMessage())
                                    .build()
                    ));
                })
                .onErrorResume(e -> {
                    logger.error("Unexpected error during registration for username {}", registrationRequest.getUsername(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            AuthResponse.builder()
                                    .message("An error occurred during registration: " + e.getMessage())
                                    .build()
                    ));
                });
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Login request received for username: {}", loginRequest.getUsername());
        return authService.login(loginRequest)
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(IllegalArgumentException.class, e -> {
                    logger.warn("Login failed for username {}: {}", loginRequest.getUsername(), e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                            AuthResponse.builder()
                                    .message(e.getMessage())
                                    .build()
                    ));
                })
                .onErrorResume(e -> {
                    logger.error("Unexpected error during login for username {}", loginRequest.getUsername(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            AuthResponse.builder()
                                    .message("An error occurred during login: " + e.getMessage())
                                    .build()
                    ));
                });
    }
}
