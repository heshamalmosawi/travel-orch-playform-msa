package com.sayedhesham.travelorch.travel_service.service;

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
import com.sayedhesham.travelorch.travel_service.dto.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

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

    public Flux<TravelResponse> getAllTravels() {
        log.info("getAllTravels - Fetching all travels");
        return Mono.fromCallable(() -> transactionTemplate.execute(status ->
                travelRepository.findAll().stream()
                        .map(TravelResponse::fromEntity)
                        .toList()
        ))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getAllTravels - Found {} travels", list.size()))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<TravelResponse> getTravelById(Long id) {
        log.info("getTravelById - Fetching travel with id: {}", id);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            Travel travel = travelRepository.findByIdWithDestinations(id);
            if (travel == null) {
                throw new IllegalArgumentException("Travel not found with id: " + id);
            }
            return TravelResponse.fromEntity(travel);
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(t -> log.info("getTravelById - Found: {}", t.getTitle()));
    }

    public Flux<TravelResponse> getTravelsByUser(Long userId) {
        log.info("getTravelsByUser - Fetching travels for userId: {}", userId);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
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

    public Flux<TravelResponse> getTravelsByStatus(TravelStatus status) {
        log.info("getTravelsByStatus - Fetching travels with status: {}", status);
        return Mono.fromCallable(() -> transactionTemplate.execute(txStatus ->
                travelRepository.findByStatus(status).stream()
                        .map(TravelResponse::fromEntity)
                        .toList()
        ))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getTravelsByStatus - Found {} travels", list.size()))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<TravelResponse> createTravel(TravelCreateRequest request) {
        log.info("createTravel - Creating travel: {}", request.getTitle());
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.getUserId()));

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

    public Mono<TravelResponse> updateTravel(Long id, TravelUpdateRequest request) {
        log.info("updateTravel - Updating travel id: {}", id);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            Travel travel = travelRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Travel not found with id: " + id));

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

    public Mono<Void> deleteTravel(Long id) {
        log.info("deleteTravel - Deleting travel id: {}", id);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            Travel travel = travelRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Travel not found with id: " + id));

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
}
