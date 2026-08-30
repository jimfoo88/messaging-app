import { useCallback, useEffect, useMemo, useState } from 'react';
import { request } from './api';
import LoginForm from './LoginForm';
import { useAuth } from './useAuth';
import { useChatSocket } from './useChatSocket';

const addMessage = (messages, message) => messages.some((item) => item.id === message.id) ? messages : [...messages, message];
const formatTime = (value) => new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });

function ChatLayout({ auth, logout, clearAuth }) {
  const { token, user } = auth;
  const [contacts, setContacts] = useState([]);
  const [conversations, setConversations] = useState([]);
  const [activeConversation, setActiveConversation] = useState(null);
  const [messagesByConversation, setMessagesByConversation] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [onlineUsers, setOnlineUsers] = useState(() => new Set());
  const [typingByConversation, setTypingByConversation] = useState({});

  const api = useCallback((path, options = {}) => request(path, { ...options, token, onUnauthorized: clearAuth }), [token, clearAuth]);
  useEffect(() => {
    let live = true;
    Promise.all([api('/api/users'), api('/api/conversations')])
      .then(([users, loadedConversations]) => { if (live) { setContacts(users); setConversations(loadedConversations); } })
      .catch((reason) => { if (live) setError(reason.message); })
      .finally(() => { if (live) setLoading(false); });
    return () => { live = false; };
  }, [api]);

  useEffect(() => {
    if (!activeConversation) return undefined;
    let live = true;
    api(`/api/conversations/${activeConversation.id}/messages`)
      .then((messages) => { if (live) setMessagesByConversation((cache) => ({ ...cache, [activeConversation.id]: messages })); })
      .catch((reason) => { if (live) setError(reason.message); });
    return () => { live = false; };
  }, [activeConversation?.id, api]);

  const socketEvent = useCallback((event) => {
    if (event.type === 'MESSAGE_CREATED') {
      const message = event.message;
      setMessagesByConversation((cache) => ({ ...cache, [message.conversationId]: addMessage(cache[message.conversationId] || [], message) }));
      setConversations((current) => current.map((conversation) => conversation.id === message.conversationId ? { ...conversation, lastMessage: message } : conversation));
    } else if (event.type === 'PRESENCE_SNAPSHOT') setOnlineUsers(new Set(event.userIds));
    else if (event.type === 'PRESENCE_UPDATED') setOnlineUsers((current) => { const next = new Set(current); event.online ? next.add(event.userId) : next.delete(event.userId); return next; });
    else if (event.type === 'TYPING_UPDATED') {
      setTypingByConversation((current) => ({ ...current, [event.conversationId]: event.typing ? event.userId : null }));
      if (event.typing) window.setTimeout(() => setTypingByConversation((current) => current[event.conversationId] === event.userId ? { ...current, [event.conversationId]: null } : current), 5000);
    }
    else if (event.type === 'ERROR') setError('Message could not be sent.');
  }, []);
  const { connected, send } = useChatSocket(token, socketEvent, clearAuth);

  const selectContact = async (contact) => {
    setError('');
    try {
      const conversation = await api('/api/conversations/direct', { method: 'POST', body: JSON.stringify({ participantId: contact.id }) });
      setConversations((current) => current.some((item) => item.id === conversation.id) ? current : [...current, conversation]);
      setActiveConversation(conversation);
    } catch (reason) { setError(reason.message); }
  };
  const contactForConversation = (conversation) => contacts.find((contact) => conversation.participantIds.find((id) => id !== user.id) === contact.id);
  const activeContact = activeConversation && contactForConversation(activeConversation);
  const activeMessages = activeConversation ? messagesByConversation[activeConversation.id] || [] : [];
  const conversationItems = useMemo(() => conversations.map((conversation) => ({ conversation, contact: contactForConversation(conversation) })), [conversations, contacts, user.id]);

  const setTyping = (typing) => activeConversation && send({ type: 'TYPING', conversationId: activeConversation.id, typing });

  const sendMessage = (content) => {
    if (!activeConversation || !send({ type: 'SEND_MESSAGE', conversationId: activeConversation.id, content })) {
      setError('Message could not be sent.');
      return false;
    }
    return true;
  };

  return <main className="chat-shell">
    <aside className="sidebar">
      <header className="sidebar-header"><div><strong>{user.displayName}</strong><span className={connected ? 'online' : 'offline'}>{connected ? 'Connected' : 'Reconnecting…'}</span></div><button className="secondary" onClick={logout}>Log out</button></header>
      <section><h2>Contacts</h2>{loading ? <p>Loading…</p> : contacts.map((contact) => <button className="list-item" key={contact.id} onClick={() => selectContact(contact)}><strong>{contact.displayName}</strong><span className={onlineUsers.has(contact.id) ? 'online' : 'offline'}>{onlineUsers.has(contact.id) ? 'Online' : 'Offline'} · @{contact.username}</span></button>)}</section>
      <section><h2>Conversations</h2>{conversationItems.map(({ conversation, contact }) => <button className={`list-item ${activeConversation?.id === conversation.id ? 'selected' : ''}`} key={conversation.id} onClick={() => setActiveConversation(conversation)}><strong>{contact?.displayName || 'Conversation'}</strong><span>{messagesByConversation[conversation.id]?.at(-1)?.content || conversation.lastMessage?.content || 'No messages yet'}</span></button>)}</section>
    </aside>
    <section className="conversation">
      {error && <p className="error banner" role="alert">{error}</p>}
      {activeConversation ? <><header className="conversation-header"><h1>{activeContact?.displayName || 'Conversation'}</h1><span className={onlineUsers.has(activeContact?.id) ? 'online' : 'offline'}>{onlineUsers.has(activeContact?.id) ? 'Online' : 'Offline'} · @{activeContact?.username}</span></header><div className="messages">{activeMessages.length ? activeMessages.map((message) => <article key={message.id} className={`message ${message.senderId === user.id ? 'mine' : ''}`}><p>{message.content}</p><time>{formatTime(message.createdAt)}</time></article>) : <p className="empty">No messages yet. Say hello.</p>}</div>{typingByConversation[activeConversation.id] === activeContact?.id && <p className="typing">{activeContact.displayName} is typing…</p>}<MessageComposer onSend={sendMessage} onTyping={setTyping} /></> : <div className="empty-state"><h1>Select a contact</h1><p>Choose a contact to open a direct conversation.</p></div>}
    </section>
  </main>;
}

function MessageComposer({ onSend, onTyping }) {
  const [content, setContent] = useState('');
  const [error, setError] = useState('');
  const [onlineUsers, setOnlineUsers] = useState(() => new Set());
  const [typingByConversation, setTypingByConversation] = useState({});
  const submit = (event) => {
    event.preventDefault();
    const value = content.trim();
    if (!value || value.length > 2000) { setError('Messages must be between 1 and 2,000 characters.'); return; }
    if (onSend(value)) { onTyping(false); setContent(''); setError(''); }
  };
  return <form className="composer" onSubmit={submit}><label className="sr-only" htmlFor="message">Message</label><textarea id="message" value={content} onChange={(event) => { setContent(event.target.value); onTyping(event.target.value.trim().length > 0); }} placeholder="Write a message" maxLength="2000" rows="2" />{error && <span className="error">{error}</span>}<button>Send</button></form>;
}

export default function App() {
  const { auth, login, logout, clearAuth } = useAuth();
  return auth ? <ChatLayout auth={auth} logout={logout} clearAuth={clearAuth} /> : <LoginForm onLogin={login} />;
}
