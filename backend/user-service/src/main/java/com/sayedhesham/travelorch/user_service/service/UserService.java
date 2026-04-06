package com.sayedhesham.travelorch.user_service.service;

import java.time.LocalDate;

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

    public Mono<UserResponse> updateUser(Long id, UserUpdateRequest request) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

            if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
                if (userRepository.existsByUsername(request.getUsername())) {
                    throw new IllegalArgumentException("Username already exists");
                }
                user.setUsername(request.getUsername());
            }

            if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new IllegalArgumentException("Email already exists");
                }
                user.setEmail(request.getEmail());
            }

            if (request.getFirstName() != null) {
                user.setFirstName(request.getFirstName());
            }

            if (request.getLastName() != null) {
                user.setLastName(request.getLastName());
            }

            if (request.getPhone() != null) {
                user.setPhone(request.getPhone());
            }

            if (request.getDateOfBirth() != null) {
                user.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
            }

            User updatedUser = userRepository.save(user);
            return UserResponse.fromEntity(updatedUser);
        })).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteUser(Long id) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            if (!userRepository.existsById(id)) {
                throw new IllegalArgumentException("User not found with id: " + id);
            }
            userRepository.deleteById(id);
            return null;
        })).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
