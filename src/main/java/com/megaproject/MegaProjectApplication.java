package com.megaproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableMongoRepositories(basePackages = {
        "com.megaproject.auth.repository",
        "com.megaproject.profile.repository",
        "com.megaproject.jobevent.repository"
})
@EnableAsync
@EnableScheduling
public class MegaProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(MegaProjectApplication.class, args);
    }
}
