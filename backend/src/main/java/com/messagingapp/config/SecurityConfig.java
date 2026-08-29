package com.messagingapp.config;

import com.messagingapp.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class JwtFilter extends OncePerRequestFilter {
  private final UserRepository users;
  private final SecretKey key;

  JwtFilter(UserRepository users, @Value("${app.jwt.secret}") String secret) {
    this.users = users;
    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  protected void doFilterInternal(HttpServletRequest q, HttpServletResponse r, FilterChain c)
      throws ServletException, IOException {
    String h = q.getHeader("Authorization");
    try {
      if (h != null && h.startsWith("Bearer ")) {
        var u =
            users
                .findById(
                    Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(h.substring(7))
                        .getPayload()
                        .getSubject())
                .orElse(null);
        if (u != null)
          SecurityContextHolder.getContext()
              .setAuthentication(
                  new UsernamePasswordAuthenticationToken(
                      new CurrentUser(u.id(), u.username(), u.displayName()), null, List.of()));
      }
    } catch (Exception ignored) {
    }
    c.doFilter(q, r);
  }
}

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http, JwtFilter filter) throws Exception {
    return http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a -> a.requestMatchers("/api/auth/**").permitAll().anyRequest().authenticated())
        .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
