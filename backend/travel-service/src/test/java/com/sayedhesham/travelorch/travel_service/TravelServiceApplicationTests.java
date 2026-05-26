package com.sayedhesham.travelorch.travel_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires live Postgres, Elasticsearch, and Redis — run only with full infrastructure")
class TravelServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
