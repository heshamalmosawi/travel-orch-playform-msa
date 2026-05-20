package com.sayedhesham.travelorch.travel_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.sayedhesham.travelorch.common.entity.travel.Destination;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.entity.travel.TravelDestination;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.enums.TravelStatus;
import com.sayedhesham.travelorch.common.repository.accommodation.TravelAccommodationRepository;
import com.sayedhesham.travelorch.common.repository.activity.TravelActivityRepository;
import com.sayedhesham.travelorch.common.repository.transportation.TransportationSegmentRepository;
import com.sayedhesham.travelorch.common.repository.travel.DestinationRepository;
import com.sayedhesham.travelorch.common.repository.travel.TravelDestinationRepository;
import com.sayedhesham.travelorch.common.repository.travel.TravelRepository;
import com.sayedhesham.travelorch.common.repository.user.UserRepository;
import com.sayedhesham.travelorch.travel_service.dto.TravelCreateRequest;
import com.sayedhesham.travelorch.travel_service.dto.TravelDestinationCreateRequest;
import com.sayedhesham.travelorch.travel_service.dto.TravelResponse;
import com.sayedhesham.travelorch.travel_service.dto.TravelUpdateRequest;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class TravelService {

    private static final Logger log = LoggerFactory.getLogger(TravelService.class);

    private final TravelRepository travelRepository;
    private final TravelDestinationRepository travelDestinationRepository;
    private final TravelActivityRepository travelActivityRepository;
    private final TravelAccommodationRepository travelAccommodationRepository;
    private final TransportationSegmentRepository transportationSegmentRepository;
    private final DestinationRepository destinationRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    @PreAuthorize("hasPermission('travels', 'read')")
    public Flux<TravelResponse> getAllTravels() {
        log.info("getAllTravels - Fetching all travels");
        return Mono.fromCallable(() -> transactionTemplate.execute(status
                -> travelRepository.findAll().stream()
                        .map(TravelResponse::fromEntity)
                        .toList()
        ))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getAllTravels - Found {} travels", list.size()))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<TravelResponse> getTravelById(Long id, String currentUsername) {
        log.info("getTravelById - Fetching travel with id: {} for user: {}", id, currentUsername);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            Travel travel = travelRepository.findByIdWithDestinations(id);
            if (travel == null) {
                throw new IllegalArgumentException("Travel not found with id: " + id);
            }

            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

            boolean isOwner = travel.getUser() != null
                    && travel.getUser().getId().equals(currentUser.getId());
            boolean canReadAny = hasPermission(currentUser, "travels", "read");

            if (!isOwner && !canReadAny) {
                log.warn("getTravelById - User {} denied access to travel id: {}", currentUsername, id);
                throw new SecurityException("You do not have permission to view this travel");
            }

            return TravelResponse.fromEntity(travel);
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(t -> log.info("getTravelById - Found: {}", t.getTitle()));
    }

    public Flux<TravelResponse> getTravelsByUser(Long userId, String currentUsername) {
        log.info("getTravelsByUser - Fetching travels for userId: {} requested by: {}", userId, currentUsername);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

            boolean isOwner = currentUser.getId().equals(userId);
            boolean canReadAny = hasPermission(currentUser, "travels", "read");

            if (!isOwner && !canReadAny) {
                log.warn("getTravelsByUser - User {} denied access to userId: {}", currentUsername, userId);
                throw new SecurityException("You do not have permission to view these travels");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
            return travelRepository.findByUser(user).stream()
                    .map(TravelResponse::fromEntity)
                    .toList();
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getTravelsByUser - Found {} travels for userId: {}", list.size(), userId))
                .flatMapMany(Flux::fromIterable);
    }

    @PreAuthorize("hasPermission('travels', 'read')")
    public Flux<TravelResponse> getTravelsByStatus(TravelStatus status) {
        log.info("getTravelsByStatus - Fetching travels with status: {}", status);
        return Mono.fromCallable(() -> transactionTemplate.execute(txStatus
                -> travelRepository.findByStatus(status).stream()
                        .map(TravelResponse::fromEntity)
                        .toList()
        ))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getTravelsByStatus - Found {} travels", list.size()))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<TravelResponse> createTravel(TravelCreateRequest request, String currentUsername) {
        log.info("createTravel - Creating travel: {} by user: {}", request.getTitle(), currentUsername);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

            Long targetUserId = request.getUserId() != null ? request.getUserId() : currentUser.getId();

            boolean isSelf = currentUser.getId().equals(targetUserId);
            boolean canWriteAny = hasPermission(currentUser, "travels", "write");

            if (!isSelf && !canWriteAny) {
                log.warn("createTravel - User {} denied creating travel for userId: {}", currentUsername, targetUserId);
                throw new SecurityException("You do not have permission to create travels for other users");
            }

            User user = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + targetUserId));

            Travel travel = new Travel();
            travel.setUser(user);
            travel.setTitle(request.getTitle());
            travel.setDescription(request.getDescription());
            travel.setStartDate(request.getStartDate());
            travel.setEndDate(request.getEndDate());
            travel.setDurationDays(request.getDurationDays());
            travel.setTotalPrice(request.getTotalPrice());

            if (request.getDestinations() != null && !request.getDestinations().isEmpty()) {
                for (TravelDestinationCreateRequest destReq : request.getDestinations()) {
                    Destination destination = destinationRepository.findById(destReq.getDestinationId())
                            .orElseThrow(() -> new IllegalArgumentException(
                            "Destination not found with id: " + destReq.getDestinationId()));

                    TravelDestination td = new TravelDestination();
                    td.setDestination(destination);
                    td.setVisitOrder(destReq.getVisitOrder());
                    td.setArrivalDate(destReq.getArrivalDate());
                    td.setDepartureDate(destReq.getDepartureDate());
                    td.setNotes(destReq.getNotes());
                    travel.addDestination(td);
                }
            }

            Travel saved = travelRepository.save(travel);
            log.info("createTravel - Created travel id: {}, title: {}", saved.getId(), saved.getTitle());

            Travel reloaded = travelRepository.findByIdWithDestinations(saved.getId());
            return TravelResponse.fromEntity(reloaded != null ? reloaded : saved);
        }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<TravelResponse> updateTravel(Long id, TravelUpdateRequest request, String currentUsername) {
        log.info("updateTravel - Updating travel id: {} by user: {}", id, currentUsername);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            Travel travel = travelRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Travel not found with id: " + id));

            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

            boolean isOwner = travel.getUser() != null
                    && travel.getUser().getId().equals(currentUser.getId());
            boolean canWriteAny = hasPermission(currentUser, "travels", "write");

            if (!isOwner && !canWriteAny) {
                log.warn("updateTravel - User {} denied updating travel id: {}", currentUsername, id);
                throw new SecurityException("You do not have permission to update this travel");
            }

            if (request.getTitle() != null) {
                travel.setTitle(request.getTitle());
            }
            if (request.getDescription() != null) {
                travel.setDescription(request.getDescription());
            }
            if (request.getStartDate() != null) {
                travel.setStartDate(request.getStartDate());
            }
            if (request.getEndDate() != null) {
                travel.setEndDate(request.getEndDate());
            }
            if (request.getDurationDays() != null) {
                travel.setDurationDays(request.getDurationDays());
            }
            if (request.getTotalPrice() != null) {
                travel.setTotalPrice(request.getTotalPrice());
            }
            if (request.getStatus() != null) {
                travel.setStatus(request.getStatus());
            }

            Travel updated = travelRepository.save(travel);
            log.info("updateTravel - Updated travel id: {}", updated.getId());

            Travel reloaded = travelRepository.findByIdWithDestinations(updated.getId());
            return TravelResponse.fromEntity(reloaded != null ? reloaded : updated);
        }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteTravel(Long id, String currentUsername) {
        log.info("deleteTravel - Deleting travel id: {} by user: {}", id, currentUsername);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            Travel travel = travelRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Travel not found with id: " + id));

            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

            boolean isOwner = travel.getUser() != null
                    && travel.getUser().getId().equals(currentUser.getId());
            boolean canDeleteAny = hasPermission(currentUser, "travels", "delete");

            if (!isOwner && !canDeleteAny) {
                log.warn("deleteTravel - User {} denied deleting travel id: {}", currentUsername, id);
                throw new SecurityException("You do not have permission to delete this travel");
            }

            travelActivityRepository.deleteByTravel(travel);
            travelAccommodationRepository.deleteByTravel(travel);
            transportationSegmentRepository.deleteByTravel(travel);
            travelDestinationRepository.deleteByTravel(travel);
            travelRepository.delete(travel);

            log.info("deleteTravel - Deleted travel id: {}", id);
            return null;
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private boolean hasPermission(User user, String resource, String action) {
        return user.getRole() != null && user.getRole().getPermissions().stream()
                .anyMatch(permission
                        -> resource.equalsIgnoreCase(permission.getResource())
                && action.equalsIgnoreCase(permission.getAction())
                );
    }
}
