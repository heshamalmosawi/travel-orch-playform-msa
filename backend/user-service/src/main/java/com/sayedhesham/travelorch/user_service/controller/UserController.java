package com.sayedhesham.travelorch.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sayedhesham.travelorch.user_service.dto.UserResponse;
import com.sayedhesham.travelorch.user_service.dto.UserUpdateRequest;
import com.sayedhesham.travelorch.user_service.security.SecurityUtils;
import com.sayedhesham.travelorch.user_service.service.UserService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<UserResponse>>> getAllUsers() {
        Flux<UserResponse> users = userService.getAllUsers();
        return Mono.just(ResponseEntity.ok(users));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<UserResponse>> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e
                        -> Mono.just(ResponseEntity.notFound().build())
                );
    }

    @GetMapping("/username/{username}")
    public Mono<ResponseEntity<UserResponse>> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e
                        -> Mono.just(ResponseEntity.notFound().build())
                );
    }

    @GetMapping("/email/{email}")
    public Mono<ResponseEntity<UserResponse>> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e
                        -> Mono.just(ResponseEntity.notFound().build())
                );
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<UserResponse>> updateUser(@PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return SecurityUtils.getCurrentUsername()
                .flatMap(currentUsername -> userService.updateUser(id, request, currentUsername))
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e
                        -> Mono.just(ResponseEntity.badRequest().build())
                )
                .onErrorResume(SecurityException.class, e
                        -> Mono.just(ResponseEntity.status(403).build())
                );
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(IllegalArgumentException.class, e
                        -> Mono.just(ResponseEntity.notFound().build())
                );
    }
}
