package com.sayedhesham.travelorch.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication
@EntityScan("com.sayedhesham.travelorch.common.entity")
@EnableJpaRepositories(basePackages = {
    "com.sayedhesham.travelorch.common.repository.accommodation",
    "com.sayedhesham.travelorch.common.repository.activity",
    "com.sayedhesham.travelorch.common.repository.feedback",
    "com.sayedhesham.travelorch.common.repository.payment",
    "com.sayedhesham.travelorch.common.repository.rbac",
    "com.sayedhesham.travelorch.common.repository.transportation",
    "com.sayedhesham.travelorch.common.repository.travel",
    "com.sayedhesham.travelorch.common.repository.user",
    "com.sayedhesham.travelorch.common.repository.report"
})
@EnableNeo4jRepositories("com.sayedhesham.travelorch.common.repository.neo4j")
@ComponentScan("com.sayedhesham.travelorch")
public class PaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentServiceApplication.class, args);
	}

}
