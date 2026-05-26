package com.sayedhesham.travelorch.common.repository.elasticsearch;

import com.sayedhesham.travelorch.common.document.DestinationDocument;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DestinationElasticsearchRepository extends ReactiveElasticsearchRepository<DestinationDocument, Long> {
}
