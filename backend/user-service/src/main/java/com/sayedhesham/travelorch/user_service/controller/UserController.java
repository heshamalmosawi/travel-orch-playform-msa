package com.sayedhesham.travelorch.user_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sayedhesham.travelorch.user_service.dto.RoleUpdateRequest;
import com.sayedhesham.travelorch.user_service.dto.TravelerStatsResponse;
import com.sayedhesham.travelorch.user_service.dto.UserResponse;
import com.sayedhesham.travelorch.user_service.dto.UserUpdateRequest;
import com.sayedhesham.travelorch.user_service.security.SecurityUtils;
import com.sayedhesham.travelorch.user_service.service.TravelerStatsService;
import com.sayedhesham.travelorch.user_service.service.UserService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final TravelerStatsService travelerStatsService;

    public UserController(UserService userService, TravelerStatsService travelerStatsService) {
        this.userService = userService;
        this.travelerStatsService = travelerStatsService;
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String role) {
        if (role != null && !role.isBlank()) {
            log.info("GET /users?role={} - Fetching users by role", role);
            return SecurityUtils.getCurrentUsername()
                    .doOnNext(username -> log.debug("GET /users?role={} - Requested by user: {}", role, username))
                    .then(Mono.just(ResponseEntity.ok(userService.getUsersByRole(role))))
                    .doOnError(e -> log.error("GET /users?role={} - Error: {}", role, e.getMessage()));
        }
        log.info("GET /users - Fetching all users");
        return SecurityUtils.getCurrentUsername()
                .doOnNext(username -> log.debug("GET /users - Requested by user: {}", username))
                .then(Mono.just(ResponseEntity.ok(userService.getAllUsers())))
                .doOnNext(response -> log.info("GET /users - Returning users list"))
                .doOnError(e -> log.error("GET /users - Error: {}", e.getMessage()));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<UserResponse>> getUserById(@PathVariable Long id) {
        log.info("GET /users/{} - Fetching user by id", id);
        return userService.getUserById(id)
                .doOnNext(user -> log.info("GET /users/{} - Found user: {}", id, user.getUsername()))
                .<ResponseEntity<UserResponse>>map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("GET /users/{} - User not found", id);
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @GetMapping("/{id}/stats")
    public Mono<ResponseEntity<TravelerStatsResponse>> getUserStats(@PathVariable Long id) {
        log.info("GET /users/{}/stats - Fetching traveler stats", id);
        return SecurityUtils.getCurrentUsername()
                .doOnNext(username -> log.info("GET /users/{}/stats - Requested by user: {}", id, username))
                .flatMap(currentUsername -> travelerStatsService.getStats(id, currentUsername))
                .doOnNext(stats -> log.info("GET /users/{}/stats - Stats computed: {}", id, stats))
                .<ResponseEntity<TravelerStatsResponse>>map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("GET /users/{}/stats - Error: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("GET /users/{}/stats - Forbidden: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.status(403).build());
                });
    }

    @GetMapping("/username/{username}")
    public Mono<ResponseEntity<UserResponse>> getUserByUsername(@PathVariable String username) {
        log.info("GET /users/username/{} - Fetching user by username", username);
        return userService.getUserByUsername(username)
                .doOnNext(user -> log.info("GET /users/username/{} - Found user with id: {}", username, user.getId()))
                .<ResponseEntity<UserResponse>>map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("GET /users/username/{} - User not found", username);
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @GetMapping("/email/{email}")
    public Mono<ResponseEntity<UserResponse>> getUserByEmail(@PathVariable String email) {
        log.info("GET /users/email/{} - Fetching user by email", email);
        return userService.getUserByEmail(email)
                .doOnNext(user -> log.info("GET /users/email/{} - Found user: {}", email, user.getUsername()))
                .<ResponseEntity<UserResponse>>map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("GET /users/email/{} - User not found", email);
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @PatchMapping("/{id}")
    public Mono<ResponseEntity<UserResponse>> updateUser(@PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("PATCH /users/{} - Update request received", id);
        return SecurityUtils.getCurrentUsername()
                .doOnNext(username -> log.info("PATCH /users/{} - Update requested by: {}", id, username))
                .flatMap(currentUsername -> userService.updateUser(id, request, currentUsername))
                .doOnNext(updated -> log.info("PATCH /users/{} - User updated successfully: {}", id, updated.getUsername()))
                .<ResponseEntity<UserResponse>>map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("PATCH /users/{} - Bad request: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                })
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("PATCH /users/{} - Forbidden: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.status(403).build());
                });
    }

    @PatchMapping("/{id}/role")
    public Mono<ResponseEntity<UserResponse>> updateUserRole(@PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {
        log.info("PATCH /users/{}/role - Role update request: {}", id, request.getRole());
        return userService.updateUserRole(id, request)
                .doOnNext(updated -> log.info("PATCH /users/{}/role - Role updated for user: {}", id, updated.getUsername()))
                .<ResponseEntity<UserResponse>>map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("PATCH /users/{}/role - Bad request: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                })
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("PATCH /users/{}/role - Forbidden: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.status(403).build());
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable Long id) {
        log.info("DELETE /users/{} - Delete request received", id);
        return userService.deleteUser(id)
                .doOnSuccess(v -> log.info("DELETE /users/{} - User deleted successfully", id))
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("DELETE /users/{} - User not found", id);
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }
}
