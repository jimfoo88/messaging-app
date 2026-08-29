# Messaging App

A local Docker Compose application for one-to-one messaging. The current milestone implements the Spring Boot REST backend with MongoDB persistence and JWT authentication. The React UI and WebSocket delivery flow are not implemented yet.

## Services

| Service | Purpose |
| --- | --- |
| `nginx` | Public reverse proxy on [http://localhost:8080](http://localhost:8080) |
| `backend` | Java 21 / Spring Boot REST API |
| `mongodb` | Persistent users, conversations, and messages |
| `redis` | Reserved for presence, typing indicators, sessions, and WebSocket pub/sub |
| `frontend` | Placeholder React service; UI implementation is pending |

MongoDB and Redis use named Docker volumes, so stored data survives container recreation.

## Backend features

- BCrypt-protected seeded user passwords
- Stateless JWT authentication using `Authorization: Bearer <token>`
- MongoDB-backed users, direct conversations, and messages
- Deterministic participant ordering: a pair of users can only have one direct conversation
- Conversation membership checks before a user may view or send messages
- Message content validation (non-blank, maximum 2,000 characters)
- Password hashes are never returned by the API

WebSocket authentication, real-time delivery, delivery/read updates, online presence, and typing indicators are intentionally pending.

## Run locally

Requirements: Docker Engine with Docker Compose.

```bash
docker compose up --build
```

The API is available through Nginx at `http://localhost:8080`. To stop the stack while retaining MongoDB and Redis data:

```bash
docker compose down
```

To remove persisted local data as well:

```bash
docker compose down -v
```

## Test users

| Username | Password | Display name |
| --- | --- | --- |
| `alice` | `alice123` | Alice Adams |
| `bob` | `bob123` | Bob Brown |
| `carol` | `carol123` | Carol Chen |

The users are seeded only when the `users` collection is empty.

## REST API

All endpoints except login require a bearer token.

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Exchange username and password for a JWT |
| `GET` | `/api/users` | List contacts excluding the authenticated user |
| `GET` | `/api/conversations` | List the authenticated user's conversations |
| `POST` | `/api/conversations/direct` | Find or create a direct conversation |
| `GET` | `/api/conversations/{id}/messages` | List messages in a conversation |
| `POST` | `/api/conversations/{id}/messages` | Persist a new message in a conversation |

### Example API flow

```bash
# Log in as Alice. Copy the returned token into TOKEN.
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"alice123"}'

# Get available contacts.
curl http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN"

# Create or retrieve a conversation with Bob using Bob's id from /api/users.
curl -X POST http://localhost:8080/api/conversations/direct \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"participantId":"BOB_ID"}'

# Send a message using the returned conversation id.
curl -X POST http://localhost:8080/api/conversations/CONVERSATION_ID/messages \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"content":"Hello Bob"}'
```

## Run backend tests

Maven is not required on the host. Run the backend test suite in the same Java 21 Maven image used by the Docker build:

```bash
docker run --rm \
  -v "$PWD/backend:/app" \
  -w /app \
  maven:3.9-eclipse-temurin-21 \
  mvn test
```

The current focused test verifies the deterministic direct-conversation key. The backend image also compiles the application during `docker compose up --build`.
