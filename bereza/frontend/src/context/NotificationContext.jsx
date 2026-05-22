import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { useAuth } from './AuthContext';
import { listNotifications, unreadCount as apiUnread } from '../api/notifications';
import { awaitConnected, getStompClient } from '../ws/stomp';

const Ctx = createContext(null);

export function NotificationProvider({ children }) {
  const { user } = useAuth();
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);

  const load = useCallback(async () => {
    if (!user) return;
    const page = await listNotifications(0, 30);
    setItems(page.items || []);
    setUnread(await apiUnread());
  }, [user]);

  useEffect(() => {
    if (!user) {
      setItems([]); setUnread(0); return;
    }
    load();
    const stomp = getStompClient(user.username);

    let sub = null;
    let cleaned = false;
    (async () => {
      await awaitConnected();
      if (cleaned) return;
      sub = stomp.subscribe('/user/queue/notifications', (msg) => {
        try {
          const n = JSON.parse(msg.body);
          setItems((prev) => [n, ...prev]);
          setUnread((u) => u + 1);
          if (window.Notification?.permission === 'granted') {
            new window.Notification(n.title, { body: n.body || '' });
          }
        } catch (e) { /* noop */ }
      });
    })();

    return () => {
      cleaned = true;
      sub?.unsubscribe?.();
    };
  }, [user, load]);

  return (
    <Ctx.Provider value={{ items, unread, refresh: load, setUnread }}>
      {children}
    </Ctx.Provider>
  );
}

export const useNotifications = () => useContext(Ctx);
