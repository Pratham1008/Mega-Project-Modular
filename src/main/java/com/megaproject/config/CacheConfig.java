package com.megaproject.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager();
        mgr.registerCustomCache("profileCounts",
                Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(10).recordStats().build());

        mgr.registerCustomCache("profileSummary",
                Caffeine.newBuilder().expireAfterWrite(2, TimeUnit.MINUTES)
                        .maximumSize(5_000).recordStats().build());

        mgr.registerCustomCache("conversationList",
                Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS)
                        .maximumSize(10_000).recordStats().build());

        mgr.registerCustomCache("jobsPage",
                Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS)
                        .maximumSize(500).recordStats().build());

        mgr.registerCustomCache("eventsPage",
                Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS)
                        .maximumSize(500).recordStats().build());

        return mgr;
    }
}