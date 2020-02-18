package com.example.infinispan.config;

import static org.infinispan.configuration.cache.CacheMode.INVALIDATION_ASYNC;
import static org.infinispan.configuration.cache.CacheMode.LOCAL;
import static org.infinispan.configuration.cache.CacheMode.REPL_ASYNC;
import static org.infinispan.transaction.TransactionMode.NON_TRANSACTIONAL;

import java.util.Properties;
import java.util.Set;

import javax.persistence.SharedCacheMode;

import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.ServiceRegistry;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionType;
import org.infinispan.hibernate.cache.v53.InfinispanRegionFactory;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.spring.embedded.provider.SpringEmbeddedCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager(DefaultCacheManager defaultCacheManager) {
        CompositeCacheManager cacheManager =
                new CompositeCacheManager(new SpringEmbeddedCacheManager(defaultCacheManager));
        cacheManager.setFallbackToNoOpCache(true);
        return cacheManager;
    }

    @Bean(destroyMethod = "stop")
    public DefaultCacheManager defaultCacheManager(Set<InfinispanCacheConfigurer> configurers) throws Exception {

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        // @formatter:off
        configurationBuilder
                .jmxStatistics().enabled(true)
                .memory()
                .evictionType(EvictionType.COUNT).size(10000)
                .expiration()
                .lifespan(100000)
                .maxIdle(100000);
            configurationBuilder.persistence()
                    .passivation(true)
                    .addSingleFileStore()
                    .purgeOnStartup(true)
                    .maxEntries(10000)
                    .location(System.getProperty("java.io.tmpdir") + "/infinispan-caches");
        // @formatter:on

        GlobalConfigurationBuilder globalConfigurationBuilder = new GlobalConfigurationBuilder();
        globalConfigurationBuilder.globalJmxStatistics().jmxDomain("infinispan").enable();
        globalConfigurationBuilder.transport().defaultTransport().clusterName("infinispan-cluster");
        globalConfigurationBuilder.defaultCacheName("default");
        DefaultCacheManager cacheManager =
                new DefaultCacheManager(globalConfigurationBuilder.build(), configurationBuilder.build());
        configurers.forEach(c -> c.configureCache(cacheManager));
        return cacheManager;
    }

    @Bean
    RegionFactory infinispanCacheRegionFactory(DefaultCacheManager defaultCacheManager) {
        return new CacheRegionFactory(defaultCacheManager);
    }

    @Bean
    InfinispanCacheConfigurer hibernateL2CacheConfiguration() {

        // @formatter:off
        return m -> {
            m.defineConfiguration("entity", new ConfigurationBuilder().clustering().cacheMode(INVALIDATION_ASYNC)
                    .jmxStatistics().enabled(true)
                    .locking().concurrencyLevel(1000).lockAcquisitionTimeout(15000).template(true).build());
            m.defineConfiguration("query", new ConfigurationBuilder().clustering().cacheMode(INVALIDATION_ASYNC)
                    .jmxStatistics().enabled(true)
                    .locking().concurrencyLevel(1000).lockAcquisitionTimeout(15000).template(true).build());
            m.defineConfiguration("replicated-query", new ConfigurationBuilder().clustering().cacheMode(REPL_ASYNC)
                    .jmxStatistics().enabled(true)
                    .locking().concurrencyLevel(1000).lockAcquisitionTimeout(15000).template(true).build());
            m.defineConfiguration("timestamps", new ConfigurationBuilder().clustering().cacheMode(REPL_ASYNC)
                    .jmxStatistics().enabled(true)
                    .locking().concurrencyLevel(1000).lockAcquisitionTimeout(15000).template(true).build());
            m.defineConfiguration("pending-puts", new ConfigurationBuilder().clustering().cacheMode(LOCAL)
                    .jmxStatistics().enabled(true)
                    .simpleCache(true).transaction().transactionMode(NON_TRANSACTIONAL).expiration().maxIdle(60000)
                    .template(true).build());
        };
        // @formatter:on
    }

    @Bean
    public JpaConfiguration.JpaPropertyConfigurerAdapter infinispanJpaProperties(DefaultCacheManager cacheManager) {
        return properties -> {
            properties.put(AvailableSettings.CACHE_REGION_FACTORY, new CacheRegionFactory(cacheManager));
            properties.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, true);
            properties.put(AvailableSettings.USE_QUERY_CACHE, true);
            properties.put(AvailableSettings.GENERATE_STATISTICS, true);
            properties.put(AvailableSettings.USE_STRUCTURED_CACHE, true);
            properties.put(AvailableSettings.JPA_SHARED_CACHE_MODE, SharedCacheMode.ALL.toString());
        };
    }


    @FunctionalInterface
    public interface InfinispanCacheConfigurer {
        /**
         * Configure an Infinispan cache.
         *
         * @param manager The {@link EmbeddedCacheManager}.
         */
        void configureCache(EmbeddedCacheManager manager);
    }

    public static class CacheRegionFactory extends InfinispanRegionFactory {

        private static final long serialVersionUID = -712579631478998468L;

        private EmbeddedCacheManager cacheManager;

        CacheRegionFactory(EmbeddedCacheManager cacheManager) {
            super();
            this.cacheManager = cacheManager;
        }

        @Override
        protected EmbeddedCacheManager createCacheManager(Properties properties, ServiceRegistry serviceRegistry) {
            return cacheManager;
        }
    }
}
