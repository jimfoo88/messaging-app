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
