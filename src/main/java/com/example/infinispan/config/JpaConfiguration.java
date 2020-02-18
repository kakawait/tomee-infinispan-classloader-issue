package com.example.infinispan.config;


import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;
import static org.hibernate.cfg.AvailableSettings.SHOW_SQL;
import static org.hibernate.cfg.AvailableSettings.USE_SQL_COMMENTS;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.tool.schema.Action;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@Configuration
@EnableJpaRepositories
public class JpaConfiguration {

    private final DataSource dataSource;

    private final List<JpaPropertyConfigurerAdapter> jpaPropertyConfigurerAdapters;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public JpaConfiguration(DataSource dataSource, List<JpaPropertyConfigurerAdapter> jpaPropertyConfigurerAdapters) {
        this.dataSource = dataSource;
        this.jpaPropertyConfigurerAdapters = jpaPropertyConfigurerAdapters;
    }

    /**
     * Persistence annotations support : used for automatic EntityManager injection BeanPostProcessor must be static to
     * avoid
     */
    @Bean
    public static PersistenceAnnotationBeanPostProcessor persistenceAnnotationBeanPostProcessor() {
        return new PersistenceAnnotationBeanPostProcessor();
    }

    @Bean
    public EntityManagerFactory entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();

        entityManagerFactory.setDataSource(dataSource);
        entityManagerFactory.setPackagesToScan("com.example.infinispan");
        entityManagerFactory.setPersistenceUnitName("dsi2");
        entityManagerFactory.setJpaDialect(new HibernateJpaDialect());
        entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties jpaProperties = new Properties();
        jpaProperties.putAll(getJpaPropertyMap(jpaPropertyConfigurerAdapters));

        entityManagerFactory.setJpaProperties(jpaProperties);

        entityManagerFactory.afterPropertiesSet();

        return entityManagerFactory.getObject();
    }

    @Bean
    public JpaTransactionManager transactionManager() {
        return new JpaTransactionManager(entityManagerFactory());
    }

    public interface JpaPropertyConfigurerAdapter {
        void configure(Map<String, Object> properties);
    }

    private Map<String, Object> getJpaPropertyMap(List<JpaPropertyConfigurerAdapter> adapters) {
        Map<String, Object> result = new HashMap<>();
        result.put(AvailableSettings.JPA_PERSISTENCE_PROVIDER, HibernatePersistenceProvider.class.getCanonicalName());
        result.put(DIALECT, "org.hibernate.dialect.H2Dialect");
        result.put(USE_SQL_COMMENTS, true);
        result.put(SHOW_SQL, false);
        result.put(HBM2DDL_AUTO, Action.NONE);

        adapters.forEach(a -> a.configure(result));

        return result;
    }

}
