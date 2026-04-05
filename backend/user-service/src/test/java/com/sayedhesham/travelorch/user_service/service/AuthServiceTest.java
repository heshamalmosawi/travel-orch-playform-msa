package com.sayedhesham.travelorch.user_service.service;

import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sayedhesham.travelorch.common.entity.rbac.Role;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.repository.rbac.RoleRepository;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.common.util.jwt.JwtUtil;
import com.sayedhesham.travelorch.user_service.dto.AuthResponse;
import com.sayedhesham.travelorch.user_service.dto.RegistrationRequest;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegistrationRequest registrationRequest;
    private User savedUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        registrationRequest = RegistrationRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .build();

        userRole = Role.builder()
                .name("USER")
                .description("Regular user")
                .build();

        savedUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .roles(new HashSet<>())
                .build();
    }

    @Test
    void register_Success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyString(), anySet(), anyBoolean())).thenReturn("jwt-token");

        Mono<AuthResponse> result = authService.register(registrationRequest);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                    response.getMessage().equals("User registered successfully") &&
                    response.getUsername().equals("testuser") &&
                    response.getEmail().equals("test@example.com") &&
                    response.getToken().equals("jwt-token")
                )
                .verifyComplete();
    }

    @Test
    void register_UsernameExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        Mono<AuthResponse> result = authService.register(registrationRequest);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().equals("Username already exists")
                )
                .verify();
    }

    @Test
    void register_EmailExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        Mono<AuthResponse> result = authService.register(registrationRequest);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().equals("Email already exists")
                )
                .verify();
    }

    @Test
    void register_DefaultRoleNotFound() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        Mono<AuthResponse> result = authService.register(registrationRequest);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof IllegalStateException &&
                    throwable.getMessage().equals("Default USER role not found")
                )
                .verify();
    }
}
