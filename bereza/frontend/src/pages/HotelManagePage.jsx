import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { myHotels } from '../api/hotels';

export default function HotelManagePage() {
  const [hotels, setHotels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    let alive = true;
    setLoading(true);
    myHotels()
      .then((items) => { if (alive) setHotels(items || []); })
      .catch((e) => {
        console.error('myHotels failed', e);
        if (alive) setError(e?.response?.data?.detail || 'Не удалось загрузить список отелей');
      })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, []);

  return (
    <div className="page">
      <header className="page__header">
        <h2>Мой двор</h2>
        <button className="btn btn--primary" onClick={() => navigate('/hotels/manage/new')}>
          Возвести двор
        </button>
      </header>

      {error && <div className="alert alert--error">{error}</div>}

      {loading ? <div className="muted">Подождите…</div> : hotels.length === 0 ? (
        <div className="card">
          <div className="muted">Дворов у вас пока нет.</div>
          <p style={{ marginTop: 8 }}>
            Возведите гостинный двор: пропишите имя, град, мзду, удобства,
            прицепите изображения — и он появится среди прочих в поиске.
          </p>
        </div>
      ) : (
        <div className="grid">
          {hotels.map(h => (
            <div key={h.id} className="card hotel">
              <div className="hotel__photo" style={{ background: '#e6efe9' }}>
                {h.photos?.[0] ? <img alt={h.name} src={h.photos[0]} /> : <span className="muted">изображение</span>}
              </div>
              <div className="hotel__body">
                <div className="strong">{h.name}</div>
                <div className="muted small">{h.city} • {h.stars ? '★'.repeat(h.stars) : '—'}</div>
                <div className="price">{Number(h.pricePerNight).toLocaleString('ru-RU')} ₽ за ночь</div>
                <div className="muted small" style={{ marginTop: 6 }}>
                  Светлиц: {h.roomsAvailable} • {h.active ? 'отворён' : 'затворён'}
                </div>
                <div className="list__aside" style={{ marginTop: 8 }}>
                  <Link to={`/hotels/${h.id}`} className="btn btn--ghost btn--sm">Заглянуть</Link>
                  <Link to={`/hotels/manage/${h.id}`} className="btn btn--primary btn--sm">Переписать</Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
