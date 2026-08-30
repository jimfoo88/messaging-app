package com.messagingapp.config;

/** Allows audit logging to identify a successful result without inspecting sensitive response fields. */
public interface AuditIdentity {
  CurrentUser auditUser();
}
