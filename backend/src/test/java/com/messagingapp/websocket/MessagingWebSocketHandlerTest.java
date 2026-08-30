package com.messagingapp.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messagingapp.config.CurrentUser;
import com.messagingapp.model.Message;
import com.messagingapp.service.MessagingService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class MessagingWebSocketHandlerTest {
  @Test
  void sendMessagePersistsAndDeliversCreatedEventToParticipants() throws Exception {
    MessagingService messages = mock(MessagingService.class);
    ConnectionRegistry connections = new ConnectionRegistry();
    MessagingWebSocketHandler handler = new MessagingWebSocketHandler(new ObjectMapper().findAndRegisterModules(), messages, connections, mock(PresenceService.class), mock(TypingService.class));
    WebSocketSession alice = session("alice");
    WebSocketSession bob = session("bob");
    Message stored = new Message("message-1", "conversation-1", "alice", "Hello", Instant.now(), Message.Status.SENT);
    when(messages.send("alice", "conversation-1", "Hello")).thenReturn(stored);
    when(messages.participants("alice", "conversation-1")).thenReturn(List.of("alice", "bob"));
    handler.afterConnectionEstablished(alice);
    handler.afterConnectionEstablished(bob);

    handler.handleTextMessage(alice, new TextMessage("{\"type\":\"SEND_MESSAGE\",\"conversationId\":\"conversation-1\",\"content\":\"Hello\"}"));

    verify(messages).send("alice", "conversation-1", "Hello");
    verify(bob).sendMessage(argThat((TextMessage event) -> event.getPayload().contains("MESSAGE_CREATED") && event.getPayload().contains("message-1")));
  }

  private WebSocketSession session(String userId) {
    WebSocketSession session = mock(WebSocketSession.class);
    when(session.getAttributes()).thenReturn(Map.of("user", new CurrentUser(userId, userId, userId)));
    when(session.isOpen()).thenReturn(true);
    return session;
  }
}
