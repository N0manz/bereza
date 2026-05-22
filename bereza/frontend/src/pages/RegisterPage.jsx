import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const ROLES = [
  { value: 'TOURIST', label: 'Путник' },
  { value: 'GUIDE',   label: 'Путевождь' },
  { value: 'HOTEL',   label: 'Гостинный двор' },
];

export default function RegisterPage() {
  const { register, login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    username: '', email: '', password: '', displayName: '',
    role: 'TOURIST', phone: ''
  });
  const [error, setError] = useState(null);
  const [pending, setPending] = useState(false);

  const onChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const onSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setPending(true);
    try {
      await register(form);
      await login(form.username, form.password);
      navigate('/chats', { replace: true });
    } catch (err) {
      setError(err?.response?.data?.detail || 'Ошибка регистрации');
    } finally {
      setPending(false);
    }
  };

  return (
    <div className="auth">
      <form className="card auth__card" onSubmit={onSubmit}>
        <h1 className="auth__title">Запись в путевую книгу</h1>
        <p className="muted">Назовись путником, путевождем или гостинным двором.</p>

        <label className="field"><span>Имя</span>
          <input name="displayName" value={form.displayName} onChange={onChange} required />
        </label>
        <label className="field"><span>Прозвище (логин)</span>
          <input name="username" value={form.username} onChange={onChange} required minLength={3} />
        </label>
        <label className="field"><span>Грамота (email)</span>
          <input type="email" name="email" value={form.email} onChange={onChange} required />
        </label>
        <label className="field"><span>Тайное слово</span>
          <input type="password" name="password" value={form.password} onChange={onChange} required minLength={8} />
        </label>
        <label className="field"><span>Гонец (телефон)</span>
          <input name="phone" value={form.phone} onChange={onChange} />
        </label>
        <label className="field"><span>Я записываюсь как</span>
          <select name="role" value={form.role} onChange={onChange}>
            {ROLES.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}
          </select>
        </label>

        {error && <div className="alert alert--error">{error}</div>}
        <button className="btn btn--primary" disabled={pending}>
          {pending ? 'Записываем…' : 'Записаться в книгу'}
        </button>
        <div className="auth__hint">
          Уже записан? <Link to="/login">Войти в палаты</Link>
        </div>
      </form>
    </div>
  );
}
