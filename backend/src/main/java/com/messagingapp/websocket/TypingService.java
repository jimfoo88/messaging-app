package com.messagingapp.websocket;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TypingService {
  private final StringRedisTemplate redis;
  public TypingService(StringRedisTemplate redis) { this.redis = redis; }
  public void update(String conversationId, String userId, boolean typing) {
    String key = "typing:" + conversationId + ":" + userId;
    if (typing) redis.opsForValue().set(key, "1", Duration.ofSeconds(5)); else redis.delete(key);
  }
}
