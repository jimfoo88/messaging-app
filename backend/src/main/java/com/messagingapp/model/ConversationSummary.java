package com.messagingapp.model;

import java.time.Instant;
import java.util.List;

/** Keeps the sidebar useful after reload without loading every conversation's full history. */
public record ConversationSummary(
    String id, String participantKey, List<String> participantIds, Instant createdAt, Message lastMessage) {}
