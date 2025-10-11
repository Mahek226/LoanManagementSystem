package com.tss.springsecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("com.tss.springsecurity.entity")
public class SpringsecurityApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringsecurityApplication.class, args);
    }
}