# Frontend Handoff

This document describes the implemented React client and its backend contract.

## Scope and constraints

The client is a small local, one-to-one messaging app built with React hooks; it uses no state-management framework. The browser reaches every backend feature through Nginx on the same origin, so use relative HTTP paths such as `/api/users`.

The frontend must run as the existing Compose `frontend` service and be reachable through `http://localhost:8080` after `docker compose up --build`.

## What exists today

- Java 21 Spring Boot backend, MongoDB persistence, Redis-backed JWT revocation, and an Nginx reverse proxy.
- Three seed users: `alice/alice123`, `bob/bob123`, and `carol/carol123`.
- REST authentication, contacts, direct conversations, and message history.
- A React UI for login, contacts, conversations, message history, typing, and WebSocket messaging.
- A raw WebSocket endpoint for real-time messages, presence, and typing events.
- Redis-backed ephemeral online presence and five-second typing indicators. There is no `/api/me`, refresh-token flow, delivered/read receipt, or multi-instance socket coordination.

## Client auth lifecycle

1. Submit `POST /api/auth/login` with `{"username":"alice","password":"alice123"}`.
2. Store the returned token and user object in client state. `localStorage` is acceptable for this local take-home; never log the token.
3. Send `Authorization: Bearer <token>` on every authenticated REST request.
4. Open the socket only after login succeeds. Build its URL using the page protocol:
   - `ws://<host>/ws?token=<encoded token>` for HTTP
   - `wss://<host>/ws?token=<encoded token>` for HTTPS
5. On a `401` or `403`, close the socket, clear client auth state, and return to the login screen. A `403` after logout is expected.
6. For logout, call `POST /api/auth/logout` with the bearer token. The server stores a token fingerprint in Redis until the JWT expires. Close the WebSocket and clear local state after its `204` response.

There is no refresh-token flow. Re-login when the token becomes invalid or expires.

## REST contract

All paths below are relative to the browser origin. All endpoints except login require the bearer token.

| Request | Body | Successful response |
| --- | --- | --- |
| `POST /api/auth/login` | `{"username":"alice","password":"alice123"}` | `{"token":"...","user":{"id":"...","username":"alice","displayName":"Alice Adams"}}` |
| `POST /api/auth/logout` | none | `204 No Content` |
| `GET /api/users` | none | Contact array; the current user is excluded |
| `GET /api/conversations` | none | Conversation summaries for the current user, newest activity first, with `lastMessage` when one exists |
| `POST /api/conversations/direct` | `{"participantId":"CONTACT_ID"}` | Existing or newly-created direct conversation |
| `GET /api/conversations/{conversationId}/messages` | none | Message array, oldest first |
| `POST /api/conversations/{conversationId}/messages` | `{"content":"Hello"}` | Persisted message |

### Response shapes

```ts
type User = {
  id: string;
  username: string;
  displayName: string;
};

type Conversation = {
  id: string;
  participantKey: string;
  participantIds: string[]; // exactly two IDs for this MVP
  createdAt: string;         // ISO timestamp
  lastMessage: Message | null; // Present in GET /api/conversations summaries
};

type Message = {
  id: string;
  conversationId: string;
  senderId: string;
  content: string;
  createdAt: string;         // ISO timestamp
  status: "SENT";
};
```

Do not expose or expect a password hash. A message must be non-blank and at most 2,000 characters. The backend validates conversation membership on both REST and WebSocket sends.

## Recommended screen and data flow

1. Render the login form while no token exists.
2. After login, load contacts and conversations in parallel.
3. Selecting a contact calls `POST /api/conversations/direct`; then load that conversation’s history.
4. When sending from the composer, send the WebSocket command below. Do not add a final message to the list until its `MESSAGE_CREATED` event arrives, because the server generates the message id and timestamp.
5. When receiving `MESSAGE_CREATED`, append it only if its `conversationId` matches the active conversation; still cache it for inactive conversations so reopening them shows the new item.
6. The sender also receives `MESSAGE_CREATED`. Deduplicate by `message.id` before adding to UI state.
7. Reload history with `GET .../messages` when opening a conversation. This is the source of truth after page refresh or a socket reconnect.
8. Render contact presence from `PRESENCE_SNAPSHOT` and `PRESENCE_UPDATED` events, and remote typing from `TYPING_UPDATED`.
9. Scroll the active message pane to its bottom when history loads or a new active-conversation message arrives.

## WebSocket contract

Connect only with a valid, non-revoked login token:

```text
/ws?token=<encodeURIComponent(token)>
```

Send exactly this JSON event:

```json
{"type":"SEND_MESSAGE","conversationId":"CONVERSATION_ID","content":"Hello Bob"}
```

Successful events arrive for both participants:

```json
{
  "type":"MESSAGE_CREATED",
  "message": {
    "id":"MESSAGE_ID",
    "conversationId":"CONVERSATION_ID",
    "senderId":"SENDER_ID",
    "content":"Hello Bob",
    "createdAt":"2026-08-30T00:00:00Z",
    "status":"SENT"
  }
}
```

The server also emits these 3 presence and typing events:

```json
{"type":"PRESENCE_SNAPSHOT","userIds":["USER_ID"]}
{"type":"PRESENCE_UPDATED","userId":"USER_ID","online":true}
{"type":"TYPING_UPDATED","conversationId":"CONVERSATION_ID","userId":"USER_ID","typing":true}
```

Send a typing event while the user is composing:

```json
{"type":"TYPING","conversationId":"CONVERSATION_ID","typing":true}
```

Invalid socket commands return a deliberately generic event:

```json
{"type":"ERROR","message":"Message was not accepted"}
```

Show a non-sensitive error such as “Message could not be sent”; do not infer whether the failure was malformed input, membership, or token-related.

## Suggested component boundaries

The current UI stays small:

- `App`: authentication gate and route-level layout.
- `LoginForm`: submit credentials and display login failures.
- `ChatLayout`: owns loaded contacts/conversations and selected contact.
- `MessageComposer`: validates non-blank text and emits a send request.
- `useAuth`: token/user storage, login, logout, invalid-auth handling.
- `useChatSocket`: socket lifecycle, JSON parsing, reconnect policy, and message dispatch.

A single `useState`/`useEffect` implementation is enough for this MVP. Avoid a generic API framework or chat library.

## Browser verification checklist

Use Playwright after implementing the UI:

1. Log in as Alice in one browser context and Bob in another.
2. Select Bob from Alice’s contacts and open the direct conversation.
3. Send a message from Alice.
4. Confirm Bob receives it without reload.
5. Reload Bob’s conversation and confirm the persisted message remains.
6. Log Alice out; confirm subsequent API calls and a socket reconnect with her old token fail.

## Backend ownership boundaries

Do not duplicate backend rules in the frontend. The backend owns authentication, token revocation, conversation membership, direct-conversation uniqueness, persistence, presence, typing expiry, and message ids/timestamps. The frontend owns presentation, local cache, active-pane scrolling, and socket reconnection behavior.
