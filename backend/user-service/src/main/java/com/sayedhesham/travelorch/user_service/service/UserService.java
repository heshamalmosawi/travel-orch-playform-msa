package com.sayedhesham.travelorch.user_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    @PreAuthorize("hasPermission('user', 'list')")
    public Flux<UserResponse> getAllUsers() {
        log.info("getAllUsers - Fetching all users");
        return Mono.fromCallable(() -> transactionTemplate.execute(status
                -> userRepository.findAll().stream()
                        .map(UserResponse::fromEntity)
                        .toList()
        ))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getAllUsers - Found {} users", list.size()))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<UserResponse> getUserById(Long id) {
        log.info("getUserById - Fetching user with id: {}", id);
        return Mono.fromCallable(() -> transactionTemplate.execute(status
                -> userRepository.findById(id)
                        .map(UserResponse::fromEntity)
                        .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id))
        ))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(user -> log.info("getUserById - Found user: {}", user.getUsername()));
    }

    public Mono<UserResponse> getUserByUsername(String username) {
        log.info("getUserByUsername - Fetching user: {}", username);
        return Mono.fromCallable(() -> transactionTemplate.execute(status
                -> userRepository.findByUsername(username)
                        .map(UserResponse::fromEntity)
                        .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username))
        ))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(user -> log.info("getUserByUsername - Found user with id: {}", user.getId()));
    }

    public Mono<UserResponse> getUserByEmail(String email) {
        log.info("getUserByEmail - Fetching user with email: {}", email);
        return Mono.fromCallable(() -> transactionTemplate.execute(status
                -> userRepository.findByEmail(email)
                        .map(UserResponse::fromEntity)
                        .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email))
        ))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(user -> log.info("getUserByEmail - Found user: {}", user.getUsername()));
    }

    public Mono<UserResponse> updateUser(Long id, UserUpdateRequest request, String currentUsername) {
        log.info("updateUser - Updating user id: {} by user: {}", id, currentUsername);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User targetUser = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found: " + currentUsername));

            boolean isOwner = targetUser.getUsername().equals(currentUsername);
            boolean canUpdateAny = hasPermission(currentUser, "user", "update");
            log.debug("updateUser - isOwner: {}, canUpdateAny: {}", isOwner, canUpdateAny);

            if (!isOwner && !canUpdateAny) {
                log.warn("updateUser - User {} denied update on user id: {}", currentUsername, id);
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
                targetUser.setDateOfBirth(request.getDateOfBirth());
            }

            User updatedUser = userRepository.save(targetUser);
            log.info("updateUser - User {} updated successfully by {}", targetUser.getUsername(), currentUsername);
            return UserResponse.fromEntity(updatedUser);
        })).subscribeOn(Schedulers.boundedElastic());
    }

    @PreAuthorize("hasPermission('user', 'delete')")
    public Mono<Void> deleteUser(Long id) {
        log.info("deleteUser - Deleting user with id: {}", id);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            if (!userRepository.existsById(id)) {
                log.warn("deleteUser - User not found with id: {}", id);
                throw new IllegalArgumentException("User not found with id: " + id);
            }
            userRepository.deleteById(id);
            log.info("deleteUser - User id: {} deleted successfully", id);
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
