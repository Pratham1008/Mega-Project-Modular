package com.megaproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = {
        "com.megaproject.auth.repository",
        "com.megaproject.profile.repository",
        "com.megaproject.jobevent.repository",
        "com.megaproject.chat.repository",
        "com.megaproject.donation.repository"
})
@EnableAsync
@EnableScheduling
public class MegaProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(MegaProjectApplication.class, args);
    }
}
