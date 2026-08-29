package com.messagingapp.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("messages")
public record Message(
    @Id String id,
    @Indexed String conversationId,
    String senderId,
    String content,
    Instant createdAt,
    Status status) {
  public enum Status {
    SENT
  }
}
