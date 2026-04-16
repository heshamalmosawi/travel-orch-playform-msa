package com.sayedhesham.travelorch.travel_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.sayedhesham.travelorch.common.entity.travel.Destination;
import com.sayedhesham.travelorch.common.repository.travel.DestinationRepository;
import com.sayedhesham.travelorch.travel_service.dto.DestinationCreateRequest;
import com.sayedhesham.travelorch.travel_service.dto.DestinationResponse;
import com.sayedhesham.travelorch.travel_service.dto.DestinationUpdateRequest;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class DestinationService {

    private static final Logger log = LoggerFactory.getLogger(DestinationService.class);

    private final DestinationRepository destinationRepository;
    private final TransactionTemplate transactionTemplate;

    public Flux<DestinationResponse> getAllDestinations() {
        log.info("getAllDestinations - Fetching all destinations");
        return Mono.fromCallable(() -> transactionTemplate.execute(status ->
                destinationRepository.findAll().stream()
                        .map(DestinationResponse::fromEntity)
                        .toList()
        ))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("getAllDestinations - Found {} destinations", list.size()))
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<DestinationResponse> searchDestinations(String name, String country, String city) {
        log.info("searchDestinations - name: {}, country: {}, city: {}", name, country, city);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            if (name != null && !name.isBlank()) {
                return destinationRepository.findByNameContainingIgnoreCase(name).stream()
                        .map(DestinationResponse::fromEntity).toList();
            }
            if (country != null && !country.isBlank() && city != null && !city.isBlank()) {
                return destinationRepository.findByCountryAndCity(country, city).stream()
                        .map(DestinationResponse::fromEntity).toList();
            }
            if (country != null && !country.isBlank()) {
                return destinationRepository.findByCountry(country).stream()
                        .map(DestinationResponse::fromEntity).toList();
            }
            if (city != null && !city.isBlank()) {
                return destinationRepository.findByCity(city).stream()
                        .map(DestinationResponse::fromEntity).toList();
            }
            return destinationRepository.findAll().stream()
                    .map(DestinationResponse::fromEntity).toList();
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(list -> log.info("searchDestinations - Found {} results", list.size()))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<DestinationResponse> getDestinationById(Long id) {
        log.info("getDestinationById - Fetching destination with id: {}", id);
        return Mono.fromCallable(() -> transactionTemplate.execute(status ->
                destinationRepository.findById(id)
                        .map(DestinationResponse::fromEntity)
                        .orElseThrow(() -> new IllegalArgumentException("Destination not found with id: " + id))
        ))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(dest -> log.info("getDestinationById - Found: {}", dest.getName()));
    }

    public Mono<DestinationResponse> createDestination(DestinationCreateRequest request) {
        log.info("createDestination - Creating destination: {}", request.getName());
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            Destination destination = new Destination();
            destination.setName(request.getName());
            destination.setDescription(request.getDescription());
            destination.setCountry(request.getCountry());
            destination.setCity(request.getCity());
            destination.setRegion(request.getRegion());
            destination.setLatitude(request.getLatitude());
            destination.setLongitude(request.getLongitude());
            destination.setImageBase64(request.getImageBase64());

            Destination saved = destinationRepository.save(destination);
            log.info("createDestination - Created destination id: {}, name: {}", saved.getId(), saved.getName());
            return DestinationResponse.fromEntity(saved);
        }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<DestinationResponse> updateDestination(Long id, DestinationUpdateRequest request) {
        log.info("updateDestination - Updating destination id: {}", id);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            Destination destination = destinationRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Destination not found with id: " + id));

            if (request.getName() != null) {
                destination.setName(request.getName());
            }
            if (request.getDescription() != null) {
                destination.setDescription(request.getDescription());
            }
            if (request.getCountry() != null) {
                destination.setCountry(request.getCountry());
            }
            if (request.getCity() != null) {
                destination.setCity(request.getCity());
            }
            if (request.getRegion() != null) {
                destination.setRegion(request.getRegion());
            }
            if (request.getLatitude() != null) {
                destination.setLatitude(request.getLatitude());
            }
            if (request.getLongitude() != null) {
                destination.setLongitude(request.getLongitude());
            }
            if (request.getImageBase64() != null) {
                destination.setImageBase64(request.getImageBase64());
            }

            Destination updated = destinationRepository.save(destination);
            log.info("updateDestination - Updated destination id: {}", updated.getId());
            return DestinationResponse.fromEntity(updated);
        }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteDestination(Long id) {
        log.info("deleteDestination - Deleting destination id: {}", id);
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            if (!destinationRepository.existsById(id)) {
                log.warn("deleteDestination - Destination not found with id: {}", id);
                throw new IllegalArgumentException("Destination not found with id: " + id);
            }
            destinationRepository.deleteById(id);
            log.info("deleteDestination - Deleted destination id: {}", id);
            return null;
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
