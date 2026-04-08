package com.sayedhesham.travelorch.user_service.service;

import java.time.LocalDate;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.user_service.dto.UserResponse;
import com.sayedhesham.travelorch.user_service.dto.UserUpdateRequest;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    @PreAuthorize("hasPermission('user', 'list')")
    public Flux<UserResponse> getAllUsers() {
        return Mono.fromCallable(() -> transactionTemplate.execute(status
                -> userRepository.findAll().stream()
                        .map(UserResponse::fromEntity)
                        .toList()
        ))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<UserResponse> getUserById(Long id) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status
                -> userRepository.findById(id)
                        .map(UserResponse::fromEntity)
                        .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id))
        ))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<UserResponse> getUserByUsername(String username) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status
                -> userRepository.findByUsername(username)
                        .map(UserResponse::fromEntity)
                        .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username))
        ))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<UserResponse> getUserByEmail(String email) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status
                -> userRepository.findByEmail(email)
                        .map(UserResponse::fromEntity)
                        .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email))
        ))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<UserResponse> updateUser(Long id, UserUpdateRequest request, String currentUsername) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User targetUser = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found: " + currentUsername));

            boolean isOwner = targetUser.getUsername().equals(currentUsername);
            boolean canUpdateAny = hasPermission(currentUser, "user", "update");

            if (!isOwner && !canUpdateAny) {
                throw new SecurityException("You do not have permission to update this user");
            }

            if (request.getUsername() != null && !request.getUsername().equals(targetUser.getUsername())) {
                if (userRepository.existsByUsername(request.getUsername())) {
                    throw new IllegalArgumentException("Username already exists");
                }
                targetUser.setUsername(request.getUsername());
            }

            if (request.getEmail() != null && !request.getEmail().equals(targetUser.getEmail())) {
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new IllegalArgumentException("Email already exists");
                }
                targetUser.setEmail(request.getEmail());
            }

            if (request.getFirstName() != null) {
                targetUser.setFirstName(request.getFirstName());
            }

            if (request.getLastName() != null) {
                targetUser.setLastName(request.getLastName());
            }

            if (request.getPhone() != null) {
                targetUser.setPhone(request.getPhone());
            }

            if (request.getDateOfBirth() != null) {
                targetUser.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
            }

            User updatedUser = userRepository.save(targetUser);
            return UserResponse.fromEntity(updatedUser);
        })).subscribeOn(Schedulers.boundedElastic());
    }

    @PreAuthorize("hasPermission('user', 'delete')")
    public Mono<Void> deleteUser(Long id) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            if (!userRepository.existsById(id)) {
                throw new IllegalArgumentException("User not found with id: " + id);
            }
            userRepository.deleteById(id);
            return null;
        })).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private boolean hasPermission(User user, String resource, String action) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(permission ->
                        resource.equalsIgnoreCase(permission.getResource()) &&
                        action.equalsIgnoreCase(permission.getAction())
                );
    }
}
