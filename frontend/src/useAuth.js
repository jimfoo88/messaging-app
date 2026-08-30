import { useCallback, useEffect, useState } from 'react';
import { request } from './api';

const STORAGE_KEY = 'messaging-app-auth';
function storedAuth() {
  //store in session cookie for future update
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY)) || null; }
  catch { localStorage.removeItem(STORAGE_KEY); return null; }
}
export function useAuth() {
  const [auth, setAuth] = useState(storedAuth);
  useEffect(() => {
    if (auth) localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
    else localStorage.removeItem(STORAGE_KEY);
  }, [auth]);
  const clearAuth = useCallback(() => setAuth(null), []);
  const login = useCallback(async (username, password) => {
    setAuth(await request('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }));
  }, []);
  const logout = useCallback(async () => {
    if (!auth?.token) return;
    try { await request('/api/auth/logout', { method: 'POST', token: auth.token }); }
    finally { clearAuth(); }
  }, [auth?.token, clearAuth]);
  return { auth, login, logout, clearAuth };
}
