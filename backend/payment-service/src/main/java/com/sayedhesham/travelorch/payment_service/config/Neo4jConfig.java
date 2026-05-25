package com.sayedhesham.travelorch.payment_service.config;

import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;

/**
 * Binds {@link Neo4jTemplate} to a dedicated {@link Neo4jTransactionManager}.
 *
 * <p>This service also uses Spring Data JPA, so Spring Boot auto-configures a JPA
 * {@code PlatformTransactionManager}. Neo4j's transaction-manager auto-configuration is
 * {@code @ConditionalOnMissingBean(PlatformTransactionManager.class)} and therefore never
 * runs once JPA's manager exists. That left the auto-configured {@code Neo4jTemplate}
 * without a transaction template, so every Neo4j {@code @Query} threw an NPE in
 * {@code Neo4jTemplate.toExecutableQuery} (its transaction template was {@code null}).
 *
 * <p>We override the template here and pass a {@link Neo4jTransactionManager} to its
 * constructor (which populates the transaction templates). The manager is kept local —
 * not registered as a bean — so it does not add a second {@code PlatformTransactionManager}
 * candidate, which would otherwise disable the auto-configured {@code TransactionTemplate}
 * the services rely on for Postgres.
 */
@Configuration
public class Neo4jConfig {

    @Bean
    public Neo4jTemplate neo4jTemplate(Neo4jClient neo4jClient,
                                       Neo4jMappingContext neo4jMappingContext,
                                       Driver driver,
                                       DatabaseSelectionProvider databaseSelectionProvider) {
        Neo4jTransactionManager transactionManager =
                new Neo4jTransactionManager(driver, databaseSelectionProvider);
        return new Neo4jTemplate(neo4jClient, neo4jMappingContext, transactionManager);
    }
}
