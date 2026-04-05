package com.sayedhesham.travelorch.user_service.controller;

import com.sayedhesham.travelorch.user_service.dto.AuthResponse;
import com.sayedhesham.travelorch.user_service.dto.LoginRequest;
import com.sayedhesham.travelorch.user_service.dto.RegistrationRequest;
import com.sayedhesham.travelorch.user_service.service.AuthService;
import jakarta.validation.Valid;
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

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(@Valid @RequestBody RegistrationRequest registrationRequest) {
        return authService.register(registrationRequest)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.badRequest().body(
                                AuthResponse.builder()
                                        .message(e.getMessage())
                                        .build()
                        ))
                )
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                AuthResponse.builder()
                                        .message("An error occurred during registration")
                                        .build()
                        ))
                );
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest)
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                AuthResponse.builder()
                                        .message(e.getMessage())
                                        .build()
                        ))
                )
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                AuthResponse.builder()
                                        .message("An error occurred during login")
                                        .build()
                        ))
                );
    }
}
