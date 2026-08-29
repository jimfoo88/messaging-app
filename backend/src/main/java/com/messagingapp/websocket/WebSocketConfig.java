package com.messagingapp.websocket;

import com.messagingapp.config.JwtAuthenticator;
import com.messagingapp.config.CurrentUser;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
  private final MessagingWebSocketHandler handler;
  private final JwtAuthenticator authenticator;

  public WebSocketConfig(MessagingWebSocketHandler handler, JwtAuthenticator authenticator) {
    this.handler = handler;
    this.authenticator = authenticator;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(handler, "/ws").addInterceptors(new JwtHandshakeInterceptor(authenticator));
  }

  private static class JwtHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtAuthenticator authenticator;

    JwtHandshakeInterceptor(JwtAuthenticator authenticator) { this.authenticator = authenticator; }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Map<String, Object> attributes) {
      String token = request.getURI().getQuery() == null ? null : java.util.Arrays.stream(request.getURI().getQuery().split("&"))
          .filter(value -> value.startsWith("token=")).map(value -> value.substring(6)).findFirst().orElse(null);
      if (token == null) return false;
      return authenticator.authenticate(java.net.URLDecoder.decode(token, java.nio.charset.StandardCharsets.UTF_8))
          .map(user -> { attributes.put("user", user); return true; }).orElse(false);
    }

    @Override public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Exception exception) {}
  }
}
