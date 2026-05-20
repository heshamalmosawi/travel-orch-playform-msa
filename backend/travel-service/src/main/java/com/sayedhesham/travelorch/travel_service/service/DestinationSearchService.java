package com.sayedhesham.travelorch.travel_service.service;

import com.sayedhesham.travelorch.common.document.DestinationDocument;
import com.sayedhesham.travelorch.common.repository.elasticsearch.DestinationElasticsearchRepository;
import com.sayedhesham.travelorch.travel_service.dto.DestinationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.PhrasePrefix;

@Service
public class DestinationSearchService {

    private static final Logger log = LoggerFactory.getLogger(DestinationSearchService.class);

    private final DestinationElasticsearchRepository esRepository;
    private final ReactiveElasticsearchTemplate esTemplate;

    public DestinationSearchService(DestinationElasticsearchRepository esRepository,
                                    ReactiveElasticsearchTemplate esTemplate) {
        this.esRepository = esRepository;
        this.esTemplate = esTemplate;
    }

    public Flux<DestinationResponse> autocomplete(String prefix) {
        log.info("autocomplete - prefix: {}", prefix);
        if (prefix == null || prefix.isBlank()) {
            return Flux.empty();
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .multiMatch(mm -> mm
                                .fields("name^3", "city^2", "country", "region")
                                .query(prefix.trim())
                                .type(PhrasePrefix)
                                .maxExpansions(10)
                        )
                )
                .withMaxResults(10)
                .build();

        return esTemplate.search(query, DestinationDocument.class)
                .map(hit -> DestinationResponse.fromDocument(hit.getContent()))
                .doOnComplete(() -> log.info("autocomplete - completed for prefix: {}", prefix));
    }

    public Mono<Void> sync(DestinationDocument doc) {
        log.info("sync - indexing destination id: {}, name: {}", doc.getId(), doc.getName());
        return esRepository.save(doc).then();
    }

    public Mono<Void> delete(Long id) {
        log.info("delete - removing destination from index id: {}", id);
        return esRepository.deleteById(id);
    }

    public Mono<Void> reindexAll(Flux<DestinationDocument> docs) {
        log.info("reindexAll - bulk indexing destinations");
        return esRepository.deleteAll()
                .thenMany(esRepository.saveAll(docs))
                .then()
                .doOnSuccess(v -> log.info("reindexAll - completed"));
    }
}
