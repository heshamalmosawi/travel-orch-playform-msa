package com.sayedhesham.travelorch.user_service.controller;

import com.sayedhesham.travelorch.user_service.dto.AuthResponse;
import com.sayedhesham.travelorch.user_service.dto.RegistrationRequest;
import com.sayedhesham.travelorch.user_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegistrationRequest registrationRequest) {
        try {
            AuthResponse response = authService.register(registrationRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder()
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    AuthResponse.builder()
                            .message("An error occurred during registration")
                            .build()
            );
        }
    }
}
