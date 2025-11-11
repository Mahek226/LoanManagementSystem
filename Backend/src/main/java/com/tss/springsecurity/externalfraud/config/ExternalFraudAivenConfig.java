package com.tss.springsecurity.externalfraud.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import javax.sql.DataSource;

/**
 * Alternative External Database Configuration
 * Use this when you want to switch back to Aiven cloud database
 */
@Configuration
@ConditionalOnProperty(name = "external-fraud.use-aiven", havingValue = "true", matchIfMissing = false)
@Slf4j
public class ExternalFraudAivenConfig {

    @Bean(name = "externalDataSource")
    public DataSource externalDataSource() {
        log.info("Connecting to Aiven MySQL cloud database...");

        return DataSourceBuilder.create()
                .url("jdbc:mysql://mysql-509864-lms-external-db.b.aivencloud.com:25060/defaultdb")
                .username("avnaadmin")
                .password("AVNS_rqE__ioF5VVnjsU01bl")
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }
}
