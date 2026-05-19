package com.sayedhesham.travelorch.user_service.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.sayedhesham.travelorch.common.entity.rbac.Role;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.repository.rbac.RoleRepository;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.user_service.dto.RoleUpdateRequest;
import com.sayedhesham.travelorch.user_service.dto.UserResponse;
import com.sayedhesham.travelorch.user_service.dto.UserUpdateRequest;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User adminUser;
    private Role userRole;
    private Role adminRole;
    private Role travelManagerRole;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        userRole = Role.builder()
                .name("user")
                .description("Regular user")
                .build();

        adminRole = Role.builder()
                .name("admin")
                .description("Administrator")
                .permissions(Set.of(
                        com.sayedhesham.travelorch.common.entity.rbac.Permission.builder()
                                .name("users.write")
                                .resource("users")
                                .action("write")
                                .build(),
                        com.sayedhesham.travelorch.common.entity.rbac.Permission.builder()
                                .name("admin.all")
                                .resource("admin")
                                .action("all")
                                .build()
                ))
                .build();

        travelManagerRole = Role.builder()
                .name("travel_manager")
                .description("Travel manager")
                .build();

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .role(userRole)
                .build();
        testUser.setId(1L);

        adminUser = User.builder()
                .username("admin")
                .email("admin@example.com")
                .passwordHash("encodedPassword")
                .firstName("Admin")
                .lastName("User")
                .role(adminRole)
                .build();
        adminUser.setId(2L);
    }

    private void setupTransactionTemplateInvocation() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    void getAllUsers_Success() {
        setupTransactionTemplateInvocation();
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        Flux<UserResponse> result = userService.getAllUsers();

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals("testuser", response.getUsername());
                    assertEquals("test@example.com", response.getEmail());
                    assertTrue(response.getRole().contains("user"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getAllUsers_EmptyList() {
        setupTransactionTemplateInvocation();
        when(userRepository.findAll()).thenReturn(List.of());

        Flux<UserResponse> result = userService.getAllUsers();

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void getUserById_Success() {
        setupTransactionTemplateInvocation();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

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
        setupTransactionTemplateInvocation();
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        StepVerifier.create(userService.getUserById(99L))
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("User not found with id: 99")
                )
                .verify();
    }

    @Test
    void getUserByUsername_Success() {
        setupTransactionTemplateInvocation();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        StepVerifier.create(userService.getUserByUsername("testuser"))
                .expectNextMatches(response -> {
                    assertEquals("testuser", response.getUsername());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getUserByUsername_NotFound() {
        setupTransactionTemplateInvocation();
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        StepVerifier.create(userService.getUserByUsername("unknown"))
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("User not found with username: unknown")
                )
                .verify();
    }

    @Test
    void getUserByEmail_Success() {
        setupTransactionTemplateInvocation();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        StepVerifier.create(userService.getUserByEmail("test@example.com"))
                .expectNextMatches(response -> {
                    assertEquals("test@example.com", response.getEmail());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getUserByEmail_NotFound() {
        setupTransactionTemplateInvocation();
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        StepVerifier.create(userService.getUserByEmail("unknown@example.com"))
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("User not found with email: unknown@example.com")
                )
                .verify();
    }

    @Test
    void updateUser_UpdateFirstName_Success() {
        setupTransactionTemplateInvocation();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

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

        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_DuplicateUsername() {
        setupTransactionTemplateInvocation();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("takenuser")).thenReturn(true);

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
        setupTransactionTemplateInvocation();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

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
        setupTransactionTemplateInvocation();
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

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
        setupTransactionTemplateInvocation();
        when(userRepository.existsById(1L)).thenReturn(true);

        StepVerifier.create(userService.deleteUser(1L))
                .verifyComplete();

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_NotFound() {
        setupTransactionTemplateInvocation();
        when(userRepository.existsById(99L)).thenReturn(false);

        StepVerifier.create(userService.deleteUser(99L))
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("User not found with id: 99")
                )
                .verify();
    }

    @Test
    void updateUserRole_Success() {
        setupTransactionTemplateInvocation();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("travel_manager")).thenReturn(Optional.of(travelManagerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleUpdateRequest request = RoleUpdateRequest.builder()
                .role("travel_manager")
                .build();

        StepVerifier.create(userService.updateUserRole(1L, request))
                .expectNextMatches(response -> {
                    assertEquals("travel_manager", response.getRole());
                    return true;
                })
                .verifyComplete();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserRole_UserNotFound() {
        setupTransactionTemplateInvocation();
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RoleUpdateRequest request = RoleUpdateRequest.builder()
                .role("travel_manager")
                .build();

        StepVerifier.create(userService.updateUserRole(99L, request))
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("User not found with id: 99")
                )
                .verify();
    }

    @Test
    void updateUserRole_RoleNotFound() {
        setupTransactionTemplateInvocation();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("nonexistent")).thenReturn(Optional.empty());

        RoleUpdateRequest request = RoleUpdateRequest.builder()
                .role("nonexistent")
                .build();

        StepVerifier.create(userService.updateUserRole(1L, request))
                .expectErrorMatches(throwable
                        -> throwable instanceof IllegalArgumentException
                && throwable.getMessage().equals("Role not found: nonexistent")
                )
                .verify();
    }
}
