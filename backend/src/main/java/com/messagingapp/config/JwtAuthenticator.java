package com.messagingapp.config;

import com.messagingapp.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtAuthenticator {
  private final UserRepository users;
  private final SecretKey key;

  public JwtAuthenticator(UserRepository users, @Value("${app.jwt.secret}") String secret) {
    this.users = users;
    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public Optional<CurrentUser> authenticate(String token) {
    try {
      String userId = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
      return users.findById(userId).map(user -> new CurrentUser(user.id(), user.username(), user.displayName()));
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }
}
