package com.megaproject.config.web;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

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
                    new Index().on("expiryDate", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS));
            mongoTemplate.indexOps("refresh_tokens").createIndex(
                    new Index().on("expiryDate", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS));

            mongoTemplate.indexOps("profiles").createIndex(
                    new CompoundIndexDefinition(new org.bson.Document()
                            .append("profileType", 1).append("deleted", 1).append("approved", 1))
                            .named("idx_profile_type_deleted_approved"));

            mongoTemplate.indexOps("profiles").createIndex(
                    new CompoundIndexDefinition(new org.bson.Document()
                            .append("department", 1).append("passingYear", 1).append("deleted", 1))
                            .named("idx_dept_year_deleted"));

            mongoTemplate.indexOps("profiles").createIndex(
                    new TextIndexDefinition.TextIndexDefinitionBuilder()
                            .onField("fullName", 3f)
                            .onField("department", 2f)
                            .onField("company", 2f)
                            .onField("jobTitle", 2f)
                            .onField("location", 1f)
                            .named("text_idx_profile_search")
                            .build());

            mongoTemplate.indexOps("chat_messages").createIndex(
                    new CompoundIndexDefinition(new org.bson.Document()
                            .append("conversationId", 1).append("sentAt", 1))
                            .named("idx_conv_sent"));

            mongoTemplate.indexOps("chat_messages").createIndex(
                    new CompoundIndexDefinition(new org.bson.Document()
                            .append("conversationId", 1).append("read", 1).append("senderId", 1))
                            .named("idx_conv_read_sender"));

            mongoTemplate.indexOps("conversations").createIndex(
                    new CompoundIndexDefinition(new org.bson.Document()
                            .append("participantIds", 1).append("lastMessageAt", -1))
                            .named("idx_participant_lastmsg"));

            mongoTemplate.indexOps("jobs").createIndex(
                    new CompoundIndexDefinition(new org.bson.Document()
                            .append("active", 1).append("createdAt", -1))
                            .named("idx_jobs_active_created"));

            mongoTemplate.indexOps("events").createIndex(
                    new CompoundIndexDefinition(new org.bson.Document()
                            .append("eventDate", 1).append("createdAt", -1))
                            .named("idx_events_date"));

            log.info("MongoDB indexes initialized successfully");
        } catch (Exception e) {
            log.warn("Index init issue (may already exist): {}", e.getMessage());
        }
    }
}