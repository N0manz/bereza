import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { myBookings, incomingBookings, changeStatus } from '../api/bookings';

const STATUS_LABEL = {
  PENDING: 'В ожидании',
  CONFIRMED: 'Утверждена',
  CANCELLED: 'Отвергнута',
  COMPLETED: 'Свершена',
};

export default function BookingsPage() {
  const { user } = useAuth();
  const [tab, setTab] = useState(user?.role === 'HOTEL' ? 'incoming' : 'my');
  const [items, setItems] = useState([]);

  const refresh = async () => {
    const fn = tab === 'incoming' ? incomingBookings : myBookings;
    const page = await fn(0, 50);
    setItems(page.items || []);
  };

  useEffect(() => { refresh(); /* eslint-disable-line */ }, [tab]);

  const update = async (id, status) => {
    await changeStatus(id, status);
    await refresh();
  };

  const isHotel = user?.role === 'HOTEL' || user?.role === 'ADMIN';

  return (
    <div className="page">
      <header className="page__header"><h2>Грамоты заездные</h2></header>
      <div className="tabs">
        <button className={tab === 'my' ? 'tab tab--active' : 'tab'} onClick={() => setTab('my')}>Мои грамоты</button>
        {isHotel && (
          <button className={tab === 'incoming' ? 'tab tab--active' : 'tab'} onClick={() => setTab('incoming')}>
            Принятые во двор
          </button>
        )}
      </div>

      {items.length === 0 ? <div className="muted">Свиток пуст.</div> : (
        <ul className="list">
          {items.map(b => (
            <li key={b.id} className="list__item">
              <div className="list__row">
                <div className="list__main">
                  <div className="strong">{b.hotelName}</div>
                  <div className="muted small">
                    {b.checkIn} → {b.checkOut} • {b.guests} душ • {b.rooms} светлиц
                  </div>
                  <div className="muted small">Путник: {b.userDisplayName}</div>
                </div>
                <div className="list__aside">
                  <div className="price">{Number(b.totalPrice).toLocaleString('ru-RU')} ₽</div>
                  <div className={`tag tag--${b.status.toLowerCase()}`}>{STATUS_LABEL[b.status]}</div>
                  {b.chatId && <Link to={`/chats/${b.chatId}`} className="btn btn--ghost btn--sm">К беседе</Link>}
                  {tab === 'incoming' && b.status === 'PENDING' && (
                    <>
                      <button className="btn btn--primary btn--sm" onClick={() => update(b.id, 'CONFIRMED')}>Утвердить</button>
                      <button className="btn btn--ghost  btn--sm" onClick={() => update(b.id, 'CANCELLED')}>Отвергнуть</button>
                    </>
                  )}
                  {tab === 'my' && b.status !== 'CANCELLED' && b.status !== 'COMPLETED' && (
                    <button className="btn btn--ghost btn--sm" onClick={() => update(b.id, 'CANCELLED')}>Отозвать</button>
                  )}
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
