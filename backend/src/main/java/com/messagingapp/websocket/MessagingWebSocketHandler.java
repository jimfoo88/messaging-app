package com.messagingapp.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messagingapp.config.AuditEvent;
import com.messagingapp.config.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.messagingapp.model.Message;
import com.messagingapp.service.MessagingService;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MessagingWebSocketHandler extends TextWebSocketHandler {
  private static final Logger log = LoggerFactory.getLogger(MessagingWebSocketHandler.class);

  private final ObjectMapper mapper;
  private final MessagingService messages;
  private final ConnectionRegistry connections;
  public MessagingWebSocketHandler(ObjectMapper mapper, MessagingService messages, ConnectionRegistry connections) { this.mapper = mapper; this.messages = messages; this.connections = connections; }
  @AuditEvent(AuditEvent.Type.WEBSOCKET_CONNECTED)
  @Override public void afterConnectionEstablished(WebSocketSession session) { connections.add(user(session).id(), session); }
  @AuditEvent(AuditEvent.Type.WEBSOCKET_DISCONNECTED)
  @Override public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) { connections.remove(user(session).id(), session); }
  @Override protected void handleTextMessage(WebSocketSession session, TextMessage payload) throws IOException {
    try {
      SendMessage command = mapper.readValue(payload.getPayload(), SendMessage.class);
      if (!"SEND_MESSAGE".equals(command.type())) throw new IllegalArgumentException("Unsupported event type");
      Message message = messages.send(user(session).id(), command.conversationId(), command.content());
      String event = mapper.writeValueAsString(new MessageCreated("MESSAGE_CREATED", message));
      for (String participant : messages.participants(user(session).id(), command.conversationId())) connections.send(participant, event);
    } catch (Exception exception) {
      log.warn("WebSocket message rejected for userId={}", user(session).id(), exception);
      session.sendMessage(new TextMessage(mapper.writeValueAsString(new ErrorEvent("ERROR", "Message was not accepted")))); }
  }
  private CurrentUser user(WebSocketSession session) { return (CurrentUser) session.getAttributes().get("user"); }
  public record SendMessage(String type, String conversationId, String content) {}
  public record MessageCreated(String type, Message message) {}
  public record ErrorEvent(String type, String message) {}
}
