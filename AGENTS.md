# Agent Instructions

## General

You are implementing a senior full stack engineer take-home assignment.

Priorities, in order:

1. Working MVP
2. Correctness
3. Simplicity
4. Testability
5. Clear architecture
6. Documentation

Do not over-engineer.

Before large changes:
- inspect existing code
- explain the intended change
- implement the smallest solution

After changes:
- compile/build
- run relevant tests
- inspect logs
- fix failures

Never silently change the architecture.

## Backend

Use:
- Java 21
- Spring Boot
- Spring Security
- MongoDB
- Redis

Keep packages simple:

controller/
service/
repository/
model/
config/
websocket/

Use SOLID principles, GOF design patterns where applicable and ensure classes are testable

## WebSocket

Implement the messaging flow directly.

Do not use a complete chat framework.

The server must:
1. authenticate connection
2. validate sender
3. validate conversation membership
4. persist message
5. deliver message to recipient
6. update delivery/read state

## Frontend

Use React.js. Current npm version is 11.19.0 and node version is 24.

Keep components small and understandable.

Avoid unnecessary state-management frameworks.

## Docker

The application must run with:

docker compose up --build

No service should require manual configuration after startup.

Docker Compose version is v5.4.0

## Verification

Use Playwright MCP to verify:
- login
- contact selection
- opening conversation
- sending message
- second browser receiving message
- message persistence after reload

## Documentation

Comments should explain WHY, not WHAT.

Example:
"Use deterministic participant ordering so the same pair of users cannot accidentally create two conversation identifiers."

Avoid comments like:
"Set variable x to y."

## Interview readiness

Prefer code that a developer can explain and modify manually.

Do not create abstractions unless they have a clear purpose.

## Current project state (update when behavior changes)

The current milestone is a REST and WebSocket backend; the React UI remains a placeholder.

### Runtime topology

- `nginx` exposes port `8080` and proxies `/api/` and `/ws/` to `backend`.
- `backend` is Spring Boot on Java 21 and uses MongoDB for users, conversations, and messages.
- `mongodb` is pinned to `mongo:8.2`; do not downgrade to `8.0`, which cannot start on this environment's Linux kernel.
- `redis` is present but is intentionally unused until presence, typing, and cross-instance WebSocket delivery are added.

### API and data behavior

- Login: `POST /api/auth/login`; all other REST endpoints require a bearer JWT.
- Seed data is created only for an empty users collection: `alice/alice123`, `bob/bob123`, and `carol/carol123`.
- `MessagingService` is the single authority for direct conversation creation, membership checks, and message persistence. Keep WebSocket and REST message creation routed through it.
- Direct conversation keys sort the two user IDs before joining them. This prevents a pair of users from receiving duplicate direct conversations.
- Do not serialize `User.passwordHash`; the model marks it with `@JsonIgnore`.

### WebSocket contract

- Connect to `/ws?token=<JWT>`. The handshake interceptor validates this JWT because browser WebSocket clients cannot set an Authorization header directly.
- Clients send `{"type":"SEND_MESSAGE","conversationId":"...","content":"..."}`.
- On success the server persists the message then sends `{"type":"MESSAGE_CREATED","message":{...}}` to connected sessions belonging to both conversation participants.
- Invalid authentication, malformed events, or authorization/persistence failures receive a generic `ERROR` event. Avoid exposing internal authorization details over the socket.
- Delivery/read state, online presence, typing indicators, Redis pub/sub, and multi-instance socket coordination remain pending.

### Verification

- Run backend tests in the Java 21 Maven container:
  `docker run --rm -v "$PWD/backend:/app" -w /app maven:3.9-eclipse-temurin-21 mvn test`
- `MessagingWebSocketHandlerTest` proves the `SEND_MESSAGE` → persistence → recipient `MESSAGE_CREATED` flow.
- Run the complete local stack with `docker compose up --build`.
