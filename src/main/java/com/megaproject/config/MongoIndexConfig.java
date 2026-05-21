package com.megaproject.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void initIndexes() {

        
        try {
            
            mongoTemplate.indexOps("otps").createIndex(
                    new Index().on("expiryDate", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS)
            );
            
            
            mongoTemplate.indexOps("refresh_tokens").createIndex(
                    new Index().on("expiryDate", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS)
            );
            

        } catch (Exception e) {
            log.warn("Failed to create TTL indexes. This might happen if an index with the same name already exists but with different options. Error: {}", e.getMessage());
        }
    }
}
