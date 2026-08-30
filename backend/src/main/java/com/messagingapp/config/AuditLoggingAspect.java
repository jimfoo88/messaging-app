package com.messagingapp.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Aspect
@Component
public class AuditLoggingAspect {
  private static final Logger log = LoggerFactory.getLogger(AuditLoggingAspect.class);

  @Around("@annotation(event)")
  public Object audit(ProceedingJoinPoint joinPoint, AuditEvent event) throws Throwable {
    Object result = joinPoint.proceed();
    if (event.value() == AuditEvent.Type.LOGIN && result instanceof ResponseEntity<?> response) {
      if (response.getStatusCode().is2xxSuccessful()) log.info("User login succeeded");
      else log.warn("User login rejected");
      return result;
    }
    String userId = userId(joinPoint.getArgs());
    log.info("Audit event={} userId={}", event.value(), userId == null ? "unknown" : userId);
    return result;
  }

  @AfterThrowing(pointcut = "execution(* com.messagingapp.controller..*(..)) || execution(* com.messagingapp.service..*(..))", throwing = "error")
  public void failure(JoinPoint joinPoint, Throwable error) {
    log.warn("Application error in {}: {}", joinPoint.getSignature().toShortString(), error.getMessage());
  }

  private String userId(Object[] args) {
    for (Object arg : args) {
      if (arg instanceof CurrentUser user) return user.id();
      if (arg instanceof WebSocketSession session && session.getAttributes().get("user") instanceof CurrentUser user) return user.id();
    }
    return null;
  }
}
