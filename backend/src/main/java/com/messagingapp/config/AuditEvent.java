package com.messagingapp.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditEvent {
  Type value();

  enum Type {
    LOGIN,
    LOGOUT,
    WEBSOCKET_CONNECTED,
    WEBSOCKET_DISCONNECTED
  }
}
