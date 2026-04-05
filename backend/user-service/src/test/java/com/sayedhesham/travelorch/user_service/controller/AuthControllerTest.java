package com.sayedhesham.travelorch.user_service.controller;

import com.sayedhesham.travelorch.user_service.dto.AuthResponse;
import com.sayedhesham.travelorch.user_service.dto.LoginRequest;
import com.sayedhesham.travelorch.user_service.dto.RegistrationRequest;
import com.sayedhesham.travelorch.user_service.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private WebTestClient webTestClient;
    private RegistrationRequest registrationRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;
    private AuthResponse loginResponse;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(authController).build();

        registrationRequest = RegistrationRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .build();

        authResponse = AuthResponse.builder()
                .message("User registered successfully")
                .username("testuser")
                .email("test@example.com")
                .token("jwt-token")
                .build();

        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        loginResponse = AuthResponse.builder()
                .message("Login successful")
                .username("testuser")
                .email("test@example.com")
                .token("jwt-token")
                .build();
    }

    @Test
    void register_Success() {
        when(authService.register(any(RegistrationRequest.class)))
                .thenReturn(Mono.just(authResponse));

        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registrationRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .value(response -> {
                    assert response.getMessage().equals("User registered successfully");
                    assert response.getUsername().equals("testuser");
                    assert response.getEmail().equals("test@example.com");
                    assert response.getToken().equals("jwt-token");
                });
    }

    @Test
    void register_UsernameExists() {
        when(authService.register(any(RegistrationRequest.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Username already exists")));

        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registrationRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(AuthResponse.class)
                .value(response -> {
                    assert response.getMessage().equals("Username already exists");
                });
    }

    @Test
    void register_InvalidRequest_MissingUsername() {
        RegistrationRequest invalidRequest = RegistrationRequest.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .build();

        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void register_InvalidRequest_ShortPassword() {
        RegistrationRequest invalidRequest = RegistrationRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("short")
                .firstName("John")
                .lastName("Doe")
                .build();

        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void register_InvalidRequest_InvalidEmail() {
        RegistrationRequest invalidRequest = RegistrationRequest.builder()
                .username("testuser")
                .email("invalid-email")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .build();

        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void login_Success() {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(Mono.just(loginResponse));

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .value(response -> {
                    assert response.getMessage().equals("Login successful");
                    assert response.getUsername().equals("testuser");
                    assert response.getEmail().equals("test@example.com");
                    assert response.getToken().equals("jwt-token");
                });
    }

    @Test
    void login_InvalidCredentials() {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Invalid credentials")));

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(AuthResponse.class)
                .value(response -> {
                    assert response.getMessage().equals("Invalid credentials");
                });
    }

    @Test
    void login_InvalidRequest_MissingPassword() {
        LoginRequest invalidRequest = LoginRequest.builder()
                .username("testuser")
                .build();

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
