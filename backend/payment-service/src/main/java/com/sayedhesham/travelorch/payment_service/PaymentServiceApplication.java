package com.sayedhesham.travelorch.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {
	"com.sayedhesham.travelorch.common.entity.user",
	"com.sayedhesham.travelorch.common.entity.travel",
	"com.sayedhesham.travelorch.common.entity.accommodation",
	"com.sayedhesham.travelorch.common.entity.activity",
	"com.sayedhesham.travelorch.common.entity.transportation",
	"com.sayedhesham.travelorch.common.entity.payment"
})
@EnableJpaRepositories(basePackages = {
	"com.sayedhesham.travelorch.common.repository.user",
	"com.sayedhesham.travelorch.common.repository.travel",
	"com.sayedhesham.travelorch.common.repository.accommodation",
	"com.sayedhesham.travelorch.common.repository.activity",
	"com.sayedhesham.travelorch.common.repository.transportation",
	"com.sayedhesham.travelorch.common.repository.payment"
})
public class PaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentServiceApplication.class, args);
	}

}
