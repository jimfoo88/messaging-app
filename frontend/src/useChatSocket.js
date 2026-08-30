import { useEffect, useRef, useState } from 'react';

export function useChatSocket(token, onEvent, onUnauthorized) {
  const socketRef = useRef(null);
  const eventRef = useRef(onEvent);
  const timerRef = useRef(null);
  const [connected, setConnected] = useState(false);
  useEffect(() => { eventRef.current = onEvent; }, [onEvent]);
  useEffect(() => {
    if (!token) return undefined;
    let stopped = false;
    let attempts = 0;
    const connect = () => {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const socket = new WebSocket(`${protocol}//${window.location.host}/ws?token=${encodeURIComponent(token)}`);
      socketRef.current = socket;
      socket.onopen = () => { attempts = 0; setConnected(true); };
      socket.onmessage = ({ data }) => {
        try { eventRef.current(JSON.parse(data)); } catch { /* Ignore malformed server events. */ }
      };
      socket.onclose = (event) => {
        setConnected(false);
        if (stopped) return;
        if ([1008, 4001, 4003].includes(event.code)) { onUnauthorized(); return; }
        timerRef.current = window.setTimeout(connect, Math.min(1000 * 2 ** attempts++, 10000));
      };
    };
    connect();
    return () => { stopped = true; window.clearTimeout(timerRef.current); socketRef.current?.close(); socketRef.current = null; };
  }, [token, onUnauthorized]);
  const send = (event) => {
    if (socketRef.current?.readyState !== WebSocket.OPEN) return false;
    socketRef.current.send(JSON.stringify(event));
    return true;
  };
  return { connected, send };
}
