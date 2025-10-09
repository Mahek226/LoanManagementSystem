package com.tss.springsecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.tss.springsecurity.entity")
@EnableJpaRepositories("com.tss.springsecurity.repository")
public class SpringsecurityApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringsecurityApplication.class, args);
    }
}