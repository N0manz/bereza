import { useEffect } from 'react';
import { useNotifications } from '../context/NotificationContext';
import { markNotificationRead } from '../api/notifications';

export default function NotificationsPage() {
  const { items, refresh, setUnread } = useNotifications() || {};

  useEffect(() => { refresh?.(); }, [refresh]);

  const markRead = async (id) => {
    await markNotificationRead(id);
    setUnread((u) => Math.max(0, u - 1));
    refresh?.();
  };

  return (
    <div className="page">
      <header className="page__header"><h2>Вести</h2></header>
      {(!items || items.length === 0) ? <div className="muted">Гонцов нет.</div> : (
        <ul className="list">
          {items.map(n => (
            <li key={n.id} className={n.read ? 'list__item list__item--read' : 'list__item'}>
              <div className="list__row">
                <div className="list__main">
                  <div className="strong">{n.title}</div>
                  <div className="muted small">{new Date(n.createdAt).toLocaleString('ru-RU')}</div>
                  <div>{n.body}</div>
                </div>
                {!n.read && (
                  <button className="btn btn--ghost btn--sm" onClick={() => markRead(n.id)}>
                    Прочтено
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
