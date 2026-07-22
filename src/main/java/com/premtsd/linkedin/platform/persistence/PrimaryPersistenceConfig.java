package com.premtsd.linkedin.platform.persistence;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Primary datasource for the 'feeddb' profile — the main relational store (DB A):
 * users, posts, connection requests, the durable job runtime, and the Spring Modulith
 * event registry (which binds to the {@link Primary} {@link DataSource}).
 *
 * <p>Only active under 'feeddb'. Off that profile this class is never loaded and Spring
 * Boot's normal single-datasource auto-configuration wires everything — so the standalone
 * H2 run and the test suite are completely unaffected. The moment we declare our own
 * {@code dataSource} / {@code entityManagerFactory} / {@code @EnableJpaRepositories},
 * the matching auto-configuration backs off and we own the wiring for BOTH units.
 *
 * <p>The repository/entity packages here are the exact complement of
 * {@link FeedPersistenceConfig}: everything <em>except</em> feed + notification. Adding a
 * new persistence module means adding its package to this list.
 */
@Configuration
@Profile("feeddb")
@EnableJpaRepositories(
        basePackages = {
                "com.premtsd.linkedin.user.internal",
                "com.premtsd.linkedin.post.internal",
                "com.premtsd.linkedin.connections.internal",
                "com.premtsd.linkedin.jobs.internal.persistence"
        },
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager")
class PrimaryPersistenceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.read")
    DataSourceProperties readDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Routing datasource for DB A: read-only transactions to the replica endpoint
     * ({@code spring.datasource.read}), writes/DDL to the primary ({@code spring.datasource}).
     * With no replica configured the read URL defaults to the primary, so a single-server
     * deployment behaves as before. See {@link ReadReplicaRouting}.
     */
    @Bean
    @Primary
    DataSource dataSource(
            @Qualifier("dataSourceProperties") DataSourceProperties write,
            @Qualifier("readDataSourceProperties") DataSourceProperties read) {
        return ReadReplicaRouting.of(
                write.initializeDataSourceBuilder().build(),
                read.initializeDataSourceBuilder().build());
    }

    @Bean
    @Primary
    LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("dataSource") DataSource dataSource) {
        return builder.dataSource(dataSource)
                .packages(
                        "com.premtsd.linkedin.user.internal",
                        "com.premtsd.linkedin.post.internal",
                        "com.premtsd.linkedin.connections.internal",
                        "com.premtsd.linkedin.jobs.internal.domain")
                .persistenceUnit("primary")
                .build();
    }

    @Bean
    @Primary
    PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }
}
