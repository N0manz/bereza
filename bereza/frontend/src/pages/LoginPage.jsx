import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState(null);
  const [pending, setPending] = useState(false);

  const onChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const onSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setPending(true);
    try {
      await login(form.username, form.password);
      if (window.Notification?.permission === 'default') {
        window.Notification.requestPermission().catch(() => {});
      }
      navigate('/chats', { replace: true });
    } catch (err) {
      setError(err?.response?.data?.detail || 'Не удалось войти');
    } finally {
      setPending(false);
    }
  };

  return (
    <div className="auth">
      <form className="card auth__card" onSubmit={onSubmit}>
        <h1 className="auth__title">Бёreza</h1>
        <p className="muted" style={{ textAlign: 'center' }}>Войди в палаты — да продолжишь путь свой.</p>
        <label className="field">
          <span>Прозвище</span>
          <input name="username" autoComplete="username" value={form.username} onChange={onChange} required />
        </label>
        <label className="field">
          <span>Тайное слово</span>
          <input name="password" type="password" autoComplete="current-password"
                 value={form.password} onChange={onChange} required />
        </label>
        {error && <div className="alert alert--error">{error}</div>}
        <button className="btn btn--primary" disabled={pending}>{pending ? 'Отворяем врата…' : 'Войти'}</button>
        <div className="auth__hint">
          Ещё не записан? <Link to="/register">В путевую книгу</Link>
        </div>
      </form>
    </div>
  );
}
