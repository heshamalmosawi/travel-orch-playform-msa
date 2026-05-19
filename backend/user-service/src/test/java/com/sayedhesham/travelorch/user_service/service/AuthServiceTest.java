package com.sayedhesham.travelorch.user_service.service;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.sayedhesham.travelorch.common.entity.rbac.Role;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.repository.rbac.RoleRepository;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.common.util.jwt.JwtUtil;
import com.sayedhesham.travelorch.user_service.dto.AuthResponse;
import com.sayedhesham.travelorch.user_service.dto.LoginRequest;
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

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private AuthService authService;

    private RegistrationRequest registrationRequest;
    private LoginRequest loginRequest;
    private User savedUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        registrationRequest = RegistrationRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .build();

        userRole = Role.builder()
                .name("user")
                .description("Regular user")
                .build();

        savedUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .role(userRole)
                .build();

        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();
    }

    @Test
    void register_Success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("user")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyString(), anyString(), anyBoolean())).thenReturn("jwt-token");

        Mono<AuthResponse> result = authService.register(registrationRequest);

        StepVerifier.create(result)
                .expectNextMatches(response
                        -> response.getMessage().equals("User registered successfully")
                && response.getUsername().equals("testuser")
                && response.getEmail().equals("test@example.com")
                && response.getToken().equals("jwt-token")
                )
                .verifyComplete();
    }

    @Test
    void register_UsernameExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        Mono<AuthResponse> result = authService.register(registrationRequest);

        StepVerifier.create(result)
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("Username already exists")
                )
                .verify();
    }

    @Test
    void register_EmailExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        Mono<AuthResponse> result = authService.register(registrationRequest);

        StepVerifier.create(result)
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("Email already exists")
                )
                .verify();
    }

    @Test
    void register_DefaultRoleNotFound() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("user")).thenReturn(Optional.empty());

        Mono<AuthResponse> result = authService.register(registrationRequest);

        StepVerifier.create(result)
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalStateException
                && throwable.getMessage().equals("Role not found: user")
                )
                .verify();
    }

    @Test
    void login_Success_WithUsername() {
        savedUser.setRole(userRole);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString(), anyBoolean())).thenReturn("jwt-token");

        Mono<AuthResponse> result = authService.login(loginRequest);

        StepVerifier.create(result)
                .expectNextMatches(response
                        -> response.getMessage().equals("Login successful")
                && response.getUsername().equals("testuser")
                && response.getEmail().equals("test@example.com")
                && response.getToken().equals("jwt-token")
                )
                .verifyComplete();
    }

    @Test
    void login_Success_WithEmail() {
        savedUser.setRole(userRole);

        LoginRequest emailLoginRequest = LoginRequest.builder()
                .username("test@example.com")
                .password("password123")
                .build();

        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString(), anyBoolean())).thenReturn("jwt-token");

        Mono<AuthResponse> result = authService.login(emailLoginRequest);

        StepVerifier.create(result)
                .expectNextMatches(response
                        -> response.getMessage().equals("Login successful")
                && response.getUsername().equals("testuser")
                && response.getEmail().equals("test@example.com")
                && response.getToken().equals("jwt-token")
                )
                .verifyComplete();
    }

    @Test
    void login_InvalidCredentials_UserNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("testuser")).thenReturn(Optional.empty());

        Mono<AuthResponse> result = authService.login(loginRequest);

        StepVerifier.create(result)
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("Invalid credentials")
                )
                .verify();
    }

    @Test
    void login_InvalidCredentials_WrongPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        Mono<AuthResponse> result = authService.login(loginRequest);

        StepVerifier.create(result)
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("Invalid credentials")
                )
                .verify();
    }

    @Test
    void register_WithTravelManagerRole_Success() {
        Role travelManagerRole = Role.builder()
                .name("travel_manager")
                .description("Travel manager")
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("travel_manager")).thenReturn(Optional.of(travelManagerRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyString(), anyString(), anyBoolean())).thenReturn("jwt-token");

        RegistrationRequest requestWithRole = RegistrationRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .role("travel_manager")
                .build();

        Mono<AuthResponse> result = authService.register(requestWithRole);

        StepVerifier.create(result)
                .expectNextMatches(response
                        -> response.getMessage().equals("User registered successfully")
                && response.getUsername().equals("testuser")
                && response.getEmail().equals("test@example.com")
                && response.getToken().equals("jwt-token")
                )
                .verifyComplete();
    }

    @Test
    void register_WithAdminRole_Failed() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

        RegistrationRequest requestWithAdminRole = RegistrationRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .role("admin")
                .build();

        Mono<AuthResponse> result = authService.register(requestWithAdminRole);

        StepVerifier.create(result)
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("Invalid role: admin. Only 'user' and 'travel_manager' are allowed for self-registration.")
                )
                .verify();
    }

    @Test
    void register_WithEmptyRole_DefaultsToUser() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("user")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyString(), anyString(), anyBoolean())).thenReturn("jwt-token");

        RegistrationRequest requestWithEmptyRole = RegistrationRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .role("")
                .build();

        Mono<AuthResponse> result = authService.register(requestWithEmptyRole);

        StepVerifier.create(result)
                .expectNextMatches(response
                        -> response.getMessage().equals("User registered successfully")
                && response.getUsername().equals("testuser")
                && response.getEmail().equals("test@example.com")
                && response.getToken().equals("jwt-token")
                )
                .verifyComplete();
    }

    @Test
    void register_WithNullRole_DefaultsToUser() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("user")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyString(), anyString(), anyBoolean())).thenReturn("jwt-token");

        RegistrationRequest requestWithNullRole = RegistrationRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .role(null)
                .build();

        Mono<AuthResponse> result = authService.register(requestWithNullRole);

        StepVerifier.create(result)
                .expectNextMatches(response
                        -> response.getMessage().equals("User registered successfully")
                && response.getUsername().equals("testuser")
                && response.getEmail().equals("test@example.com")
                && response.getToken().equals("jwt-token")
                )
                .verifyComplete();
    }

    @Test
    void register_WithInvalidRole_Failed() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

        RegistrationRequest requestWithInvalidRole = RegistrationRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .role("nonexistent")
                .build();

        Mono<AuthResponse> result = authService.register(requestWithInvalidRole);

        StepVerifier.create(result)
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("Invalid role: nonexistent. Only 'user' and 'travel_manager' are allowed for self-registration.")
                )
                .verify();
    }

    @Test
    void register_WithNonexistentRole_Failed() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("travel_manager")).thenReturn(Optional.empty());

        RegistrationRequest requestWithRole = RegistrationRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .role("travel_manager")
                .build();

        Mono<AuthResponse> result = authService.register(requestWithRole);

        StepVerifier.create(result)
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalStateException
                && throwable.getMessage().equals("Role not found: travel_manager")
                )
                .verify();
    }
}
