package com.messagingapp.websocket;

import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {
  private static final String ONLINE_USERS = "presence:online-users";
  private final StringRedisTemplate redis;
  public PresenceService(StringRedisTemplate redis) { this.redis = redis; }
  public boolean connect(String userId, String sessionId) {
    Boolean added = redis.opsForSet().add("presence:connections:" + userId, sessionId) > 0;
    return added && redis.opsForSet().add(ONLINE_USERS, userId) > 0;
  }
  public boolean disconnect(String userId, String sessionId) {
    redis.opsForSet().remove("presence:connections:" + userId, sessionId);
    if (!Boolean.TRUE.equals(redis.hasKey("presence:connections:" + userId))) return redis.opsForSet().remove(ONLINE_USERS, userId) > 0;
    return false;
  }
  public Set<String> onlineUsers() { return redis.opsForSet().members(ONLINE_USERS); }
}
