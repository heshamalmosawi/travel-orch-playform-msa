package com.sayedhesham.travelorch.user_service.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import com.sayedhesham.travelorch.common.entity.rbac.Role;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.user_service.dto.UserResponse;
import com.sayedhesham.travelorch.user_service.dto.UserUpdateRequest;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
                .name("USER")
                .description("Regular user")
                .build();

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .roles(roles)
                .build();
    }

    @Test
    void getAllUsers_Success() {
        when(transactionTemplate.execute(any())).thenReturn(
                List.of(UserResponse.fromEntity(testUser))
        );

        Flux<UserResponse> result = userService.getAllUsers();

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals("testuser", response.getUsername());
                    assertEquals("test@example.com", response.getEmail());
                    assertTrue(response.getRoles().contains("USER"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getAllUsers_EmptyList() {
        when(transactionTemplate.execute(any())).thenReturn(List.of());

        Flux<UserResponse> result = userService.getAllUsers();

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void getUserById_Success() {
        when(transactionTemplate.execute(any())).thenReturn(UserResponse.fromEntity(testUser));

        StepVerifier.create(userService.getUserById(1L))
                .expectNextMatches(response -> {
                    assertEquals("testuser", response.getUsername());
                    assertEquals("John", response.getFirstName());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getUserById_NotFound() {
        when(transactionTemplate.execute(any()))
                .thenThrow(new IllegalArgumentException("User not found with id: 99"));

        StepVerifier.create(userService.getUserById(99L))
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("User not found with id: 99")
                )
                .verify();
    }

    @Test
    void getUserByUsername_Success() {
        when(transactionTemplate.execute(any())).thenReturn(UserResponse.fromEntity(testUser));

        StepVerifier.create(userService.getUserByUsername("testuser"))
                .expectNextMatches(response -> {
                    assertEquals("testuser", response.getUsername());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getUserByUsername_NotFound() {
        when(transactionTemplate.execute(any()))
                .thenThrow(new IllegalArgumentException("User not found with username: unknown"));

        StepVerifier.create(userService.getUserByUsername("unknown"))
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("User not found with username: unknown")
                )
                .verify();
    }

    @Test
    void getUserByEmail_Success() {
        when(transactionTemplate.execute(any())).thenReturn(UserResponse.fromEntity(testUser));

        StepVerifier.create(userService.getUserByEmail("test@example.com"))
                .expectNextMatches(response -> {
                    assertEquals("test@example.com", response.getEmail());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getUserByEmail_NotFound() {
        when(transactionTemplate.execute(any()))
                .thenThrow(new IllegalArgumentException("User not found with email: unknown@example.com"));

        StepVerifier.create(userService.getUserByEmail("unknown@example.com"))
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("User not found with email: unknown@example.com")
                )
                .verify();
    }

    @Test
    void updateUser_UpdateFirstName_Success() {
        User updatedUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .firstName("Jane")
                .lastName("Doe")
                .phone("1234567890")
                .roles(testUser.getRoles())
                .build();

        when(transactionTemplate.execute(any())).thenReturn(UserResponse.fromEntity(updatedUser));

        UserUpdateRequest request = UserUpdateRequest.builder()
                .firstName("Jane")
                .build();

        StepVerifier.create(userService.updateUser(1L, request, "testuser"))
                .expectNextMatches(response -> {
                    assertEquals("Jane", response.getFirstName());
                    assertEquals("Doe", response.getLastName());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void updateUser_DuplicateUsername() {
        when(transactionTemplate.execute(any()))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("takenuser")
                .build();

        StepVerifier.create(userService.updateUser(1L, request, "testuser"))
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("Username already exists")
                )
                .verify();
    }

    @Test
    void updateUser_DuplicateEmail() {
        when(transactionTemplate.execute(any()))
                .thenThrow(new IllegalArgumentException("Email already exists"));

        UserUpdateRequest request = UserUpdateRequest.builder()
                .email("taken@example.com")
                .build();

        StepVerifier.create(userService.updateUser(1L, request, "testuser"))
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("Email already exists")
                )
                .verify();
    }

    @Test
    void updateUser_NotFound() {
        when(transactionTemplate.execute(any()))
                .thenThrow(new IllegalArgumentException("User not found with id: 99"));

        UserUpdateRequest request = UserUpdateRequest.builder()
                .firstName("Jane")
                .build();

        StepVerifier.create(userService.updateUser(99L, request, "testuser"))
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("User not found with id: 99")
                )
                .verify();
    }

    @Test
    void deleteUser_Success() {
        when(transactionTemplate.execute(any())).thenReturn(null);

        StepVerifier.create(userService.deleteUser(1L))
                .verifyComplete();
    }

    @Test
    void deleteUser_NotFound() {
        when(transactionTemplate.execute(any()))
                .thenThrow(new IllegalArgumentException("User not found with id: 99"));

        StepVerifier.create(userService.deleteUser(99L))
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("User not found with id: 99")
                )
                .verify();
    }
}
