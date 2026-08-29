# Senior Full Stack Engineer Take-Home

## Objective

Build a local Docker Compose full-stack 1-to-1 real-time messaging web app. Frontend, backend, and other required services must be running within docker containers, not standalone.

## Required services

1. frontend
   - React.js
   - chat UI
   - login
   - contacts

2. backend
   - Spring Boot
   - REST API
   - WebSocket
   - authentication/authorization
   - business logic
   - MongoDB integration
   - Redis integration

3. mongodb
   - users
   - conversations
   - messages
   - message state

4. redis
   - online presence
   - typing indicators
   - WebSocket pub/sub
   - transient/session/cache state
   - session management (if required)

5. nginx
   - reverse proxy
   - serve frontend
   - proxy REST API
   - proxy WebSocket

## MVP

Must support:

1. Login
2. Contact list
3. One-to-one conversations
4. Sending messages
5. Real-time message delivery between two browser windows
6. Persistent messages
7. Basic delivered/read status
8. Online presence
9. Typing indicator
10. Docker Compose startup

## Constraints

- No external SaaS
- Must run locally
- Do not use a complete chat/messaging framework
- Keep WebSocket/message-routing implementation understandable
- Prefer simple architecture over abstraction
- Do not over-engineer
- Every important architectural decision should be documented
- The project must be explainable manually in a follow-up interview

## Non-goals

Do NOT implement:

- group chat
- file uploads
- push notifications
- password reset
- email verification
- OAuth/SAML
- Kubernetes
- distributed microservices beyond the required containers
- end-to-end encryption
- elaborate UI
