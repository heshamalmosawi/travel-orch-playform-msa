package com.sayedhesham.travelorch.travel_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication
@EntityScan("com.sayedhesham.travelorch.common.entity")
@EnableJpaRepositories("com.sayedhesham.travelorch.common.repository")
@EnableNeo4jRepositories("com.sayedhesham.travelorch.common.repository.neo4j")
public class TravelServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TravelServiceApplication.class, args);
	}

}
