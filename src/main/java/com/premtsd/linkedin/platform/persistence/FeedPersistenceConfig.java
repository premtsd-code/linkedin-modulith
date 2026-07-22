package com.premtsd.linkedin.platform.persistence;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Dedicated second Postgres (DB B) for the feed + notification modules only, active under
 * the 'feeddb' profile. Keeps {@code FeedEntry}/{@code Notification} relational but on a
 * SEPARATE database that can be scaled and operated independently of the primary.
 *
 * <p>Binds {@code FeedEntryRepository} + {@code NotificationRepository} to this unit via
 * {@code feedEntityManagerFactory}/{@code feedTransactionManager}. The
 * {@code JpaFeedStore}/{@code JpaNotificationStore} adapters name
 * {@code feedTransactionManager} on their {@code @Transactional} methods so each store
 * method runs in a single DB-B transaction. Off the 'feeddb' profile that same name is
 * aliased to the primary transaction manager by {@link FeedTxManagerAlias}, so the default
 * single-database run behaves exactly as before.
 *
 * <p>Beans here are non-primary; the primary datasource is {@link PrimaryPersistenceConfig}.
 */
@Configuration
@Profile("feeddb")
@EnableJpaRepositories(
        basePackages = {
                "com.premtsd.linkedin.feed.internal.persistence",
                "com.premtsd.linkedin.notification.internal"
        },
        entityManagerFactoryRef = "feedEntityManagerFactory",
        transactionManagerRef = "feedTransactionManager")
class FeedPersistenceConfig {

    @Bean
    @ConfigurationProperties("feed.datasource")
    DataSourceProperties feedDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("feed.datasource.read")
    DataSourceProperties feedReadDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Routing datasource for DB B: read-only transactions to the feed replica endpoint
     * ({@code feed.datasource.read}), writes/DDL to the feed primary ({@code feed.datasource}).
     * With no replica configured the read URL defaults to the primary. See {@link ReadReplicaRouting}.
     */
    @Bean
    DataSource feedDataSource(
            @Qualifier("feedDataSourceProperties") DataSourceProperties write,
            @Qualifier("feedReadDataSourceProperties") DataSourceProperties read) {
        return ReadReplicaRouting.of(
                write.initializeDataSourceBuilder().build(),
                read.initializeDataSourceBuilder().build());
    }

    @Bean
    LocalContainerEntityManagerFactoryBean feedEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("feedDataSource") DataSource dataSource) {
        return builder.dataSource(dataSource)
                .packages(
                        "com.premtsd.linkedin.feed.internal.persistence",
                        "com.premtsd.linkedin.notification.internal")
                .persistenceUnit("feed")
                .build();
    }

    @Bean
    PlatformTransactionManager feedTransactionManager(
            @Qualifier("feedEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }
}
