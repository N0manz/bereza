import { useEffect, useState } from 'react';
import { me, updateMe } from '../api/users';
import { useAuth } from '../context/AuthContext';
import { roleLabel } from '../utils/labels';

export default function ProfilePage() {
  const { refresh } = useAuth();
  const [user, setUser] = useState(null);
  const [form, setForm] = useState({ displayName: '', phone: '', avatarUrl: '' });
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    me().then((u) => {
      setUser(u);
      setForm({ displayName: u.displayName || '', phone: u.phone || '', avatarUrl: u.avatarUrl || '' });
    });
  }, []);

  if (!user) return <div className="page"><div className="muted">Подождите…</div></div>;

  const onSave = async (e) => {
    e.preventDefault();
    const updated = await updateMe(form);
    setUser(updated);
    await refresh();
    setSaved(true);
    setTimeout(() => setSaved(false), 1500);
  };

  return (
    <div className="page page--narrow">
      <header className="page__header"><h2>Светлица</h2></header>
      <form className="card" onSubmit={onSave}>
        <div className="profile__head">
          <div className="avatar avatar--xl">{user.displayName?.[0]}</div>
          <div>
            <div className="strong">@{user.username}</div>
            <div className="muted small">{user.email} • {roleLabel(user.role)}</div>
          </div>
        </div>
        <label className="field"><span>Имя</span>
          <input value={form.displayName} onChange={(e) => setForm((f) => ({ ...f, displayName: e.target.value }))} />
        </label>
        <label className="field"><span>Гонец (телефон)</span>
          <input value={form.phone} onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))} />
        </label>
        <label className="field"><span>Лик (URL аватара)</span>
          <input value={form.avatarUrl} onChange={(e) => setForm((f) => ({ ...f, avatarUrl: e.target.value }))} />
        </label>
        <button className="btn btn--primary" type="submit">Сберечь</button>
        {saved && <div className="alert alert--ok">Сбережено</div>}
      </form>
    </div>
  );
}
