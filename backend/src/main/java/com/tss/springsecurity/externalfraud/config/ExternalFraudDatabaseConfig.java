package com.tss.springsecurity.externalfraud.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "external-fraud.enabled", havingValue = "true", matchIfMissing = true)
// @ConditionalOnProperty(name = "external-fraud.enabled", havingValue = "true", matchIfMissing = false)
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.tss.springsecurity.externalfraud.repository",
    entityManagerFactoryRef = "externalEntityManagerFactory",
    transactionManagerRef = "externalTransactionManager"
)
public class ExternalFraudDatabaseConfig {

    @Bean(name = "externalDataSource")
    public DataSource externalDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/external_lms?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata")
                .username("root")
                // .password("Root@12391@#15") // Original password - didn't work
                .password("Root@123")
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    @Bean(name = "externalEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean externalEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("externalDataSource") DataSource dataSource) {
        
        Map<String, Object> properties = new HashMap<>();
        // Remove explicit dialect - Hibernate will auto-detect MySQL dialect
        properties.put("hibernate.hbm2ddl.auto", "update"); // Create/update tables in local database
        properties.put("hibernate.show_sql", false);
        properties.put("hibernate.format_sql", true);
        properties.put("hibernate.connection.autocommit", false);
        properties.put("hibernate.connection.pool_size", 5);
        
        return builder
                .dataSource(dataSource)
                .packages("com.tss.springsecurity.externalfraud.entity")
                .persistenceUnit("externalFraudPersistenceUnit")
                .properties(properties)
                .build();
    }

    @Bean(name = "externalTransactionManager")
    public PlatformTransactionManager externalTransactionManager(
            @Qualifier("externalEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
