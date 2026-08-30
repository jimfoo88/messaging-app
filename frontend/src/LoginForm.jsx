import { useState } from 'react';

export default function LoginForm({ onLogin }) {
  const [username, setUsername] = useState('alice');
  const [password, setPassword] = useState('alice123');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const submit = async (event) => {
    event.preventDefault(); setSubmitting(true); setError('');
    try { await onLogin(username, password); }
    catch { setError('Login failed. Check your username and password.'); }
    finally { setSubmitting(false); }
  };
  return <main className="login-page"><form className="login-card" onSubmit={submit}>
    <h1>Messaging App</h1><p>Sign in to start a conversation.</p>
    <label>Username<input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required /></label>
    <label>Password<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required /></label>
    {error && <p className="error" role="alert">{error}</p>}
    <button disabled={submitting}>{submitting ? 'Signing in…' : 'Sign in'}</button>
    <small>Try alice / alice123 or bob / bob123</small>
  </form></main>;
}
