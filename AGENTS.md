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

The current milestone is a working React frontend, REST and WebSocket backend, a working one-to-one messaging MVP.

### Runtime topology

- `nginx` exposes port `8080`, serves the React frontend, and proxies `/api/` and `/ws` to `backend`.
- `frontend` is a Vite-built React app served by its own Nginx container.
- `backend` is Spring Boot on Java 21 and uses MongoDB for users, conversations, and messages.
- `mongodb` is pinned to `mongo:8.2`; do not downgrade to `8.0`, which cannot start on this environment's Linux kernel.
- `redis` stores revoked-token fingerprints, ephemeral online presence, and five-second typing indicators. MongoDB remains the only persistent message store.

### API and data behavior

- Login: `POST /api/auth/login`; all other REST endpoints require a bearer JWT.
- Seed data is created only for an empty users collection: `alice/alice123`, `bob/bob123`, and `carol/carol123`.
- `MessagingService` is the single authority for direct conversation creation, membership checks, and message persistence. Keep WebSocket and REST message creation routed through it.
- Direct conversation keys sort the two user IDs before joining them. This prevents a pair of users from receiving duplicate direct conversations.
- Do not serialize `User.passwordHash`; the model marks it with `@JsonIgnore`.
- Spring AOP audit logging emits INFO events for successful login, logout, and WebSocket connect/disconnect; WARN is used for rejected logins, WebSocket commands, and unhandled controller/service exceptions. Never log passwords or JWTs.
- `POST /api/auth/logout` requires the current bearer token and revokes its Redis fingerprint until the JWT expires. Reuse is rejected by both REST and WebSocket authentication.

### WebSocket contract

- Connect to `/ws?token=<JWT>`. The handshake interceptor validates this JWT because browser WebSocket clients cannot set an Authorization header directly.
- Clients send `{"type":"SEND_MESSAGE","conversationId":"...","content":"..."}`.
- On success the server persists the message then sends `{"type":"MESSAGE_CREATED","message":{...}}` to connected sessions belonging to both conversation participants.
- The server sends `PRESENCE_SNAPSHOT` on connection and broadcasts `PRESENCE_UPDATED` when a user connects or disconnects.
- Clients may send `TYPING`; the server broadcasts `TYPING_UPDATED` to conversation participants. Typing state expires after five seconds.
- Invalid authentication, malformed events, or authorization/persistence failures receive a generic `ERROR` event. Avoid exposing internal authorization details over the socket.
- Delivery/read state, Redis pub/sub, and multi-instance socket coordination remain pending.

### Frontend handoff

- `FRONTEND_HANDOFF.md` is the source of truth for API response shapes, socket events, auth state, and acceptance flow.
- Keep HTTP paths relative (`/api/...`) because Nginx provides the same-origin backend proxy. Use `ws:`/`wss:` based on the browser protocol for `/ws?token=...`.
- On logout or any 401/403, close the browser socket and clear the local token. Never keep a revoked token in UI state.

### Verification

- Run backend tests in the Java 21 Maven container:
  `docker run --rm -v "$PWD/backend:/app" -w /app maven:3.9-eclipse-temurin-21 mvn test`
- `MessagingWebSocketHandlerTest` proves the `SEND_MESSAGE` → persistence → recipient `MESSAGE_CREATED` flow.
- Run the complete local stack with `docker compose up --build`.
