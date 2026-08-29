package com.messagingapp.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class ConnectionRegistry {
  private final ConcurrentHashMap<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
  public void add(String userId, WebSocketSession session) { sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session); }
  public void remove(String userId, WebSocketSession session) { sessions.getOrDefault(userId, Set.of()).remove(session); }
  public void send(String userId, String event) throws IOException {
    for (WebSocketSession session : sessions.getOrDefault(userId, Set.of())) if (session.isOpen()) session.sendMessage(new TextMessage(event));
  }
}
