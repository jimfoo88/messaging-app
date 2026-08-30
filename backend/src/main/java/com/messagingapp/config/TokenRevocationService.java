package com.messagingapp.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenRevocationService {
  private static final String KEY_PREFIX = "revoked-token:";
  private final StringRedisTemplate redis;

  public TokenRevocationService(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public void revoke(String token, Duration remainingLifetime) {
    if (!remainingLifetime.isNegative() && !remainingLifetime.isZero()) {
      redis.opsForValue().set(KEY_PREFIX + fingerprint(token), "1", remainingLifetime);
    }
  }

  public boolean isRevoked(String token) {
    return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + fingerprint(token)));
  }

  private String fingerprint(String token) {
    try {
      return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 must be available", exception);
    }
  }
}
