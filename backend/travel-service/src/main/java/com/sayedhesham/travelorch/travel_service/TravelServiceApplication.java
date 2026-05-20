package com.sayedhesham.travelorch.travel_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication
@EntityScan("com.sayedhesham.travelorch.common.entity")
@EnableJpaRepositories(basePackages = {
    "com.sayedhesham.travelorch.common.repository.accommodation",
    "com.sayedhesham.travelorch.common.repository.activity",
    "com.sayedhesham.travelorch.common.repository.payment",
    "com.sayedhesham.travelorch.common.repository.rbac",
    "com.sayedhesham.travelorch.common.repository.transportation",
    "com.sayedhesham.travelorch.common.repository.travel",
    "com.sayedhesham.travelorch.common.repository.user"
})
@EnableNeo4jRepositories("com.sayedhesham.travelorch.common.repository.neo4j")
@EnableReactiveElasticsearchRepositories("com.sayedhesham.travelorch.common.repository.elasticsearch")
@ComponentScan("com.sayedhesham.travelorch")
public class TravelServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TravelServiceApplication.class, args);
	}

}
