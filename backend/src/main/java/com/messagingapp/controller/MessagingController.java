package com.messagingapp.controller;

import com.messagingapp.config.CurrentUser;
import com.messagingapp.model.*;
import com.messagingapp.service.MessagingService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MessagingController {
  private final MessagingService service;

  MessagingController(MessagingService s) {
    service = s;
  }

  record Direct(String participantId) {}

  record Send(String content) {}

  @GetMapping("/users")
  List<User> users(@AuthenticationPrincipal CurrentUser user) {
    return service.contacts(user.id());
  }

  @GetMapping("/conversations")
  List<Conversation> conversations(@AuthenticationPrincipal CurrentUser user) {
    return service.all(user.id());
  }

  @PostMapping("/conversations/direct")
  Conversation direct(@AuthenticationPrincipal CurrentUser user, @RequestBody Direct body) {
    return service.direct(user.id(), body.participantId());
  }

  @GetMapping("/conversations/{id}/messages")
  List<Message> messages(@AuthenticationPrincipal CurrentUser user, @PathVariable String id) {
    return service.messages(user.id(), id);
  }

  @PostMapping("/conversations/{id}/messages")
  Message send(
      @AuthenticationPrincipal CurrentUser user, @PathVariable String id, @RequestBody Send body) {
    return service.send(user.id(), id, body.content());
  }
}
