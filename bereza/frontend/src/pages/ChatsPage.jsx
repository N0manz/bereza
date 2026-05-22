import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { listChats, createChat } from '../api/chats';
import { searchUsers } from '../api/users';
import { useAuth } from '../context/AuthContext';
import { roleLabel } from '../utils/labels';

export default function ChatsPage() {
  const { user } = useAuth();
  const canCreateGroup = user?.role === 'GUIDE' || user?.role === 'ADMIN';
  const [chats, setChats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showNew, setShowNew] = useState(false);
  const navigate = useNavigate();

  const refresh = async () => {
    setLoading(true);
    try {
      const page = await listChats(0, 50);
      setChats(page.items || []);
    } finally { setLoading(false); }
  };

  useEffect(() => { refresh(); }, []);

  return (
    <div className="page">
      <header className="page__header">
        <h2>Беседы</h2>
        <button className="btn btn--primary" onClick={() => setShowNew(true)}>Завести беседу</button>
      </header>
      {loading ? <div className="muted">Подождите…</div>
        : chats.length === 0 ? <div className="muted">Бесед пока нет. Заведите первую!</div>
        : (
          <ul className="list">
            {chats.map(c => (
              <li key={c.id} className="list__item">
                <Link to={`/chats/${c.id}`} className="list__row">
                  <div className="avatar avatar--lg">{(c.title || 'Б')[0]}</div>
                  <div className="list__main">
                    <div className="strong">{c.title || (c.members?.map(m => m.displayName).join(', '))}</div>
                    <div className="muted small">
                      {c.type === 'PERSONAL' ? 'Личная беседа' : `Дружина • ${c.members?.length} душ`}
                    </div>
                  </div>
                  {c.unreadCount > 0 && <span className="badge">{c.unreadCount}</span>}
                </Link>
              </li>
            ))}
          </ul>
        )}
      {showNew && (
        <NewChatModal
          canCreateGroup={canCreateGroup}
          onClose={() => setShowNew(false)}
          onCreated={async (chat) => { setShowNew(false); await refresh(); navigate(`/chats/${chat.id}`); }}
        />
      )}
    </div>
  );
}

function NewChatModal({ canCreateGroup, onClose, onCreated }) {
  const [q, setQ] = useState('');
  const [results, setResults] = useState([]);
  const [picked, setPicked] = useState([]);
  const [title, setTitle] = useState('');
  const [error, setError] = useState(null);

  useEffect(() => {
    const t = setTimeout(async () => {
      if (q.trim().length === 0) { setResults([]); return; }
      try { const page = await searchUsers(q, 0, 10); setResults(page.items || []); }
      catch { setResults([]); }
    }, 250);
    return () => clearTimeout(t);
  }, [q]);

  const toggle = (u) => {
    setPicked((p) => {
      if (p.find(x => x.id === u.id)) return p.filter(x => x.id !== u.id);
      if (!canCreateGroup) return [u]; // обычный пользователь — только один собеседник
      return [...p, u];
    });
  };

  const submit = async () => {
    if (picked.length === 0) return;
    const isGroup = picked.length > 1;
    if (isGroup && !canCreateGroup) {
      setError('Групповые чаты могут создавать только гиды');
      return;
    }
    setError(null);
    try {
      const chat = await createChat({
        type: isGroup ? 'GROUP' : 'PERSONAL',
        title: isGroup ? (title || picked.map(p => p.displayName).join(', ')) : null,
        memberIds: picked.map(p => p.id),
      });
      onCreated(chat);
    } catch (e) {
      setError(e?.response?.data?.detail || 'Не удалось создать чат');
    }
  };

  return (
    <div className="modal" role="dialog" aria-modal="true" onClick={onClose}>
      <div className="modal__card" onClick={(e) => e.stopPropagation()}>
        <h3>Завести беседу</h3>
        {!canCreateGroup && (
          <div className="muted small">
            Дозволено выбрать одного собеседника. Дружину собирают только путевожди.
          </div>
        )}
        <input className="input" placeholder="Поиск по имени…" value={q} onChange={(e) => setQ(e.target.value)} />
        <ul className="picklist">
          {results.map(u => {
            const active = picked.find(p => p.id === u.id);
            return (
              <li key={u.id} className={active ? 'picklist__item picklist__item--active' : 'picklist__item'} onClick={() => toggle(u)}>
                <div className="avatar">{u.displayName[0]}</div>
                <div className="list__main">
                  <div className="strong">{u.displayName}</div>
                  <div className="muted small">@{u.username} • {roleLabel(u.role)}</div>
                </div>
              </li>
            );
          })}
        </ul>
        {canCreateGroup && picked.length > 1 && (
          <label className="field"><span>Имя дружины</span>
            <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} />
          </label>
        )}
        {error && <div className="alert alert--error">{error}</div>}
        <div className="modal__actions">
          <button className="btn btn--ghost" onClick={onClose}>Отступить</button>
          <button className="btn btn--primary" disabled={picked.length === 0} onClick={submit}>Завести</button>
        </div>
      </div>
    </div>
  );
}
