package com.sayedhesham.travelorch.travel_service.service;

import java.time.LocalDate;
import java.util.List;

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

            boolean isOwner = travel.getManager() != null
                    && travel.getManager().getId().equals(currentUser.getId());
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
            return travelRepository.findByManager(user).stream()
                    .map(TravelResponse::fromEntity)
                    .toList();
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getTravelsByUser - Found {} travels for userId: {}", list.size(), userId))
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<TravelResponse> getMyTravels(String currentUsername) {
        log.info("getMyTravels - Fetching travels for current user: {}", currentUsername);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

            boolean isManager = currentUser.getRole() != null
                    && "travel_manager".equalsIgnoreCase(currentUser.getRole().getName());
            if (!isManager) {
                log.warn("getMyTravels - User {} is not a travel manager", currentUsername);
                throw new SecurityException("Only travel managers can access this endpoint");
            }

            return travelRepository.findByManager(currentUser).stream()
                    .map(TravelResponse::fromEntity)
                    .toList();
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getMyTravels - Found {} travels for user: {}", list.size(), currentUsername))
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<TravelResponse> getUpcomingTravels() {
        log.info("getUpcomingTravels - Fetching upcoming travels");
        return Mono.fromCallable(() -> transactionTemplate.execute(status ->
                travelRepository.findAllUpcomingTravels(LocalDate.now(), TravelStatus.cancelled).stream()
                        .map(TravelResponse::fromEntity)
                        .toList()
        ))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getUpcomingTravels - Found {} upcoming travels", list.size()))
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
        log.info("createTravel - Creating travel package: {} by user: {}", request.getTitle(), currentUsername);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

            boolean canWrite = hasPermission(currentUser, "travels", "write");
            if (!canWrite) {
                log.warn("createTravel - User {} denied creating travel package", currentUsername);
                throw new SecurityException("You do not have permission to create travel packages");
            }

            User manager;
            if (request.getManagerId() != null) {
                boolean isAdmin = hasPermission(currentUser, "admin", "all");
                if (!isAdmin) {
                    log.warn("createTravel - User {} attempted to set managerId without admin permission", currentUsername);
                    throw new SecurityException("Only admins can assign an explicit managerId");
                }
                manager = userRepository.findById(request.getManagerId())
                        .orElseThrow(() -> new IllegalArgumentException("Manager not found with id: " + request.getManagerId()));
            } else {
                boolean isManager = currentUser.getRole() != null
                        && "travel_manager".equalsIgnoreCase(currentUser.getRole().getName());
                if (!isManager) {
                    log.warn("createTravel - User {} is not a travel manager and no managerId provided", currentUsername);
                    throw new SecurityException("You must be a travel manager or provide an explicit managerId");
                }
                manager = currentUser;
            }

            boolean resolvedIsManager = manager.getRole() != null
                    && "travel_manager".equalsIgnoreCase(manager.getRole().getName());
            if (!resolvedIsManager) {
                log.warn("createTravel - Resolved manager id: {} does not have travel_manager role", manager.getId());
                throw new SecurityException("The assigned manager does not have the travel_manager role");
            }

            Travel travel = new Travel();
            travel.setManager(manager);
            travel.setTitle(request.getTitle());
            travel.setDescription(request.getDescription());
            travel.setStartDate(request.getStartDate());
            travel.setEndDate(request.getEndDate());
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

            boolean isOwner = travel.getManager() != null
                    && travel.getManager().getId().equals(currentUser.getId());
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

            boolean isOwner = travel.getManager() != null
                    && travel.getManager().getId().equals(currentUser.getId());
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
