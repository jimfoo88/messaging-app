package com.messagingapp.config;

import com.messagingapp.model.User;
import com.messagingapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {
  @Bean
  CommandLineRunner seed(UserRepository users, PasswordEncoder passwords) {
    return args -> {
      if (users.count() == 0) {
        users.save(new User(null, "alice", "Alice Adams", passwords.encode("alice123")));
        users.save(new User(null, "bob", "Bob Brown", passwords.encode("bob123")));
        users.save(new User(null, "carol", "Carol Chen", passwords.encode("carol123")));
      }
    };
  }
}
