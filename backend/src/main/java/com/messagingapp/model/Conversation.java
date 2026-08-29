package com.messagingapp.model;
import java.time.Instant; import java.util.List; import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.index.Indexed; import org.springframework.data.mongodb.core.mapping.Document;
@Document("conversations") public record Conversation(@Id String id, @Indexed(unique=true) String participantKey, List<String> participantIds, Instant createdAt) {}
