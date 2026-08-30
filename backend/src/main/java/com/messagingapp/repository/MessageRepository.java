package com.messagingapp.repository;

import com.messagingapp.model.Message;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<Message, String> {
  List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);

  Message findTopByConversationIdOrderByCreatedAtDesc(String conversationId);
}
