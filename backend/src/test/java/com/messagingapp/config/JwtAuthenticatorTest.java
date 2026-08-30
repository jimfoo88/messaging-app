package com.messagingapp.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.messagingapp.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;

class JwtAuthenticatorTest {
  private static final String SECRET = "development-only-secret-change-this-to-at-least-thirty-two-characters";

  @Test
  void revokedTokenCannotAuthenticate() {
    UserRepository users = mock(UserRepository.class);
    TokenRevocationService revocations = mock(TokenRevocationService.class);
    String token = Jwts.builder().subject("alice").expiration(Date.from(Instant.now().plusSeconds(60))).signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    when(revocations.isRevoked(token)).thenReturn(true);

    assertTrue(new JwtAuthenticator(users, revocations, SECRET).authenticate(token).isEmpty());
    verifyNoInteractions(users);
  }
}
