import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';
import { roleLabel } from '../utils/labels';

export default function AppLayout() {
  const { user, logout } = useAuth();
  const { unread } = useNotifications() || { unread: 0 };
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="app">
      <aside className="sidebar">
        <Link to="/" className="brand">
          <span className="brand__mark">☘</span> Бёreza
        </Link>
        <nav>
          <NavLink to="/chats" className={({ isActive }) => isActive ? 'nav__link nav__link--active' : 'nav__link'}>
            Беседы
          </NavLink>
          <NavLink to="/hotels" className={({ isActive }) => isActive ? 'nav__link nav__link--active' : 'nav__link'}>
            Дворы
          </NavLink>
          {(user?.role === 'HOTEL' || user?.role === 'ADMIN') && (
            <NavLink to="/hotels/manage" className={({ isActive }) => isActive ? 'nav__link nav__link--active' : 'nav__link'}>
              Мой двор
            </NavLink>
          )}
          <NavLink to="/bookings" className={({ isActive }) => isActive ? 'nav__link nav__link--active' : 'nav__link'}>
            Грамоты
          </NavLink>
          <NavLink to="/notifications" className={({ isActive }) => isActive ? 'nav__link nav__link--active' : 'nav__link'}>
            Вести{unread > 0 && <span className="badge">{unread}</span>}
          </NavLink>
          <NavLink to="/profile" className={({ isActive }) => isActive ? 'nav__link nav__link--active' : 'nav__link'}>
            Светлица
          </NavLink>
        </nav>
        <div className="sidebar__bottom">
          <div className="sidebar__user">
            <div className="avatar">{user?.displayName?.[0] ?? '?'}</div>
            <div>
              <div className="strong">{user?.displayName}</div>
              <div className="muted small">{roleLabel(user?.role)}</div>
            </div>
          </div>
          <button className="btn btn--ghost" onClick={handleLogout}>Покинуть</button>
        </div>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
