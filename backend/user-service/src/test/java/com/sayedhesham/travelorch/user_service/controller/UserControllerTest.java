package com.sayedhesham.travelorch.user_service.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.sayedhesham.travelorch.user_service.dto.RoleUpdateRequest;
import com.sayedhesham.travelorch.user_service.dto.UserResponse;
import com.sayedhesham.travelorch.user_service.dto.UserUpdateRequest;
import com.sayedhesham.travelorch.user_service.service.UserService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private WebTestClient webTestClient;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(userController)
                .webFilter((exchange, chain) ->
                        chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(getAuthentication()))
                )
                .build();

        userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .role("user")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UsernamePasswordAuthenticationToken getAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "testuser", null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void getAllUsers_Success() {
        when(userService.getAllUsers()).thenReturn(Flux.just(userResponse));

        webTestClient.get()
                .uri("/users")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(UserResponse.class)
                .hasSize(1)
                .value(responses -> {
                    UserResponse response = responses.getFirst();
                    assertEquals("testuser", response.getUsername());
                    assertEquals("test@example.com", response.getEmail());
                });
    }

    @Test
    void getAllUsers_EmptyList() {
        when(userService.getAllUsers()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/users")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(UserResponse.class)
                .hasSize(0);
    }

    @Test
    void getUserById_Success() {
        when(userService.getUserById(1L)).thenReturn(Mono.just(userResponse));

        webTestClient.get()
                .uri("/users/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .value(response -> {
                    assertEquals(1L, response.getId());
                    assertEquals("testuser", response.getUsername());
                    assertEquals("test@example.com", response.getEmail());
                });
    }

    @Test
    void getUserById_NotFound() {
        when(userService.getUserById(99L))
                .thenReturn(Mono.error(new IllegalArgumentException("User not found with id: 99")));

        webTestClient.get()
                .uri("/users/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getUserByUsername_Success() {
        when(userService.getUserByUsername("testuser")).thenReturn(Mono.just(userResponse));

        webTestClient.get()
                .uri("/users/username/testuser")
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .value(response -> assertEquals("testuser", response.getUsername()));
    }

    @Test
    void getUserByUsername_NotFound() {
        when(userService.getUserByUsername("unknown"))
                .thenReturn(Mono.error(new IllegalArgumentException("User not found with username: unknown")));

        webTestClient.get()
                .uri("/users/username/unknown")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getUserByEmail_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(Mono.just(userResponse));

        webTestClient.get()
                .uri("/users/email/test@example.com")
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .value(response -> assertEquals("test@example.com", response.getEmail()));
    }

    @Test
    void getUserByEmail_NotFound() {
        when(userService.getUserByEmail("unknown@example.com"))
                .thenReturn(Mono.error(new IllegalArgumentException("User not found with email: unknown@example.com")));

        webTestClient.get()
                .uri("/users/email/unknown@example.com")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateUser_Success() {
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .build();

        UserResponse updatedResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .phone("1234567890")
                .role("user")
                .build();

        when(userService.updateUser(eq(1L), any(UserUpdateRequest.class), eq("testuser")))
                .thenReturn(Mono.just(updatedResponse));

        webTestClient.patch()
                .uri("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .value(response -> {
                    assertEquals("Jane", response.getFirstName());
                    assertEquals("Smith", response.getLastName());
                });
    }

    @Test
    void updateUser_NotFound() {
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .firstName("Jane")
                .build();

        when(userService.updateUser(eq(99L), any(UserUpdateRequest.class), eq("testuser")))
                .thenReturn(Mono.error(new IllegalArgumentException("User not found with id: 99")));

        webTestClient.patch()
                .uri("/users/99")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateUser_DuplicateUsername() {
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .username("takenuser")
                .build();

        when(userService.updateUser(eq(1L), any(UserUpdateRequest.class), eq("testuser")))
                .thenReturn(Mono.error(new IllegalArgumentException("Username already exists")));

        webTestClient.patch()
                .uri("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void deleteUser_Success() {
        when(userService.deleteUser(1L)).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/users/1")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void deleteUser_NotFound() {
        when(userService.deleteUser(99L))
                .thenReturn(Mono.error(new IllegalArgumentException("User not found with id: 99")));

        webTestClient.delete()
                .uri("/users/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateUser_WithRole_Success() {
        RoleUpdateRequest updateRequest = RoleUpdateRequest.builder()
                .role("travel_manager")
                .build();

        UserResponse updatedResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .role("travel_manager")
                .build();

        when(userService.updateUserRole(eq(1L), any(RoleUpdateRequest.class)))
                .thenReturn(Mono.just(updatedResponse));

        webTestClient.patch()
                .uri("/users/1/role")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .value(response -> {
                    assertEquals("testuser", response.getUsername());
                    assertEquals("travel_manager", response.getRole());
                });
    }

    @Test
    void updateUser_WithRole_Forbidden() {
        RoleUpdateRequest updateRequest = RoleUpdateRequest.builder()
                .role("travel_manager")
                .build();

        when(userService.updateUserRole(eq(1L), any(RoleUpdateRequest.class)))
                .thenReturn(Mono.error(new SecurityException("You do not have permission to update roles")));

        webTestClient.patch()
                .uri("/users/1/role")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isForbidden();
    }
}
