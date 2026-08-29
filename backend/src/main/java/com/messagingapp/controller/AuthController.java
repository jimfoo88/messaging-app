package com.messagingapp.controller;

import com.messagingapp.config.CurrentUser;
import com.messagingapp.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final SecretKey key;

  AuthController(UserRepository u, PasswordEncoder p, @Value("${app.jwt.secret}") String secret) {
    users = u;
    passwords = p;
    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  record Login(String username, String password) {}

  record Result(String token, CurrentUser user) {}

  @PostMapping("/login")
  ResponseEntity<Result> login(@RequestBody Login input) {
    var u =
        users
            .findByUsername(input.username() == null ? "" : input.username())
            .filter(
                x ->
                    passwords.matches(
                        input.password() == null ? "" : input.password(), x.passwordHash()))
            .orElse(null);
    if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    var principal = new CurrentUser(u.id(), u.username(), u.displayName());
    String token =
        Jwts.builder()
            .subject(u.id())
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(86400)))
            .signWith(key)
            .compact();
    return ResponseEntity.ok(new Result(token, principal));
  }
}
