<<<<<<< HEAD
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
=======
//package com.tss.springsecurity;
//
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//
//@SpringBootApplication
//public class SpringsecurityApplication {
//    public static void main(String[] args) {
//        SpringApplication.run(SpringsecurityApplication.class, args);
//    }
//}
>>>>>>> 3de8d5309d43c5ea64b2697bf8d75be5fd2d4054
