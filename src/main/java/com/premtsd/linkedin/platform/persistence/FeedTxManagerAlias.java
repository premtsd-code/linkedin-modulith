package com.premtsd.linkedin.platform.persistence;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Off the 'feeddb' profile there is only one database, so feed + notification share the
 * primary transaction manager. But {@code JpaFeedStore}/{@code JpaNotificationStore} name
 * {@code feedTransactionManager} on every {@code @Transactional} method — a name that only
 * {@link FeedPersistenceConfig} defines, and only under 'feeddb'.
 *
 * <p>This registers {@code feedTransactionManager} as an <em>alias</em> of the
 * auto-configured primary {@code transactionManager}, so the name always resolves (to the
 * same single bean) in the standalone/H2 run, the 'postgres' profile, and the test suite.
 * An alias — not a second bean — because there is genuinely one transaction manager here;
 * a delegating {@code @Bean} of the same type would try to inject itself and dead-lock.
 *
 * <p>Not loaded when 'feeddb' is active — there the real dedicated-DB manager owns the name.
 */
@Configuration
@Profile("!feeddb")
class FeedTxManagerAlias {

    @Bean
    static BeanFactoryPostProcessor feedTransactionManagerAlias() {
        return beanFactory -> {
            if (beanFactory instanceof BeanDefinitionRegistry registry) {
                registry.registerAlias("transactionManager", "feedTransactionManager");
            }
        };
    }
}
