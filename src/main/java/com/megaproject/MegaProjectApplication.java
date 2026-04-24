package com.megaproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableMongoRepositories(basePackages = {
        "com.megaproject.auth.repository",
        "com.megaproject.profile.repository",
        "com.megaproject.jobevent.repository"
})
@EnableAsync
public class MegaProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(MegaProjectApplication.class, args);
    }
}
