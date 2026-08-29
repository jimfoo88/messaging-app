package com.messagingapp.repository;
import com.messagingapp.model.Conversation; import java.util.Optional; import org.springframework.data.mongodb.repository.MongoRepository;
public interface ConversationRepository extends MongoRepository<Conversation,String> { Optional<Conversation> findByParticipantKey(String participantKey); }
