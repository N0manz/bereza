import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { searchHotels } from '../api/hotels';

export default function HotelsPage() {
  const [hotels, setHotels] = useState([]);
  const [filters, setFilters] = useState({ city: '', minPrice: '', maxPrice: '', minStars: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const search = async () => {
    setLoading(true);
    setError(null);
    try {
      const params = Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== ''));
      const page = await searchHotels({ ...params, page: 0, size: 20 });
      setHotels(page.items || []);
    } catch (e) {
      console.error('searchHotels failed', e);
      setError(e?.response?.data?.detail || e?.message || 'Не удалось загрузить отели');
      setHotels([]);
    } finally { setLoading(false); }
  };

  useEffect(() => { search(); /* eslint-disable-line */ }, []);

  return (
    <div className="page">
      <header className="page__header"><h2>Гостинные дворы</h2></header>
      <div className="filters">
        <input className="input" placeholder="Град" value={filters.city}
               onChange={(e) => setFilters((f) => ({ ...f, city: e.target.value }))} />
        <input className="input" placeholder="Мзда от" type="number" min="0" value={filters.minPrice}
               onChange={(e) => setFilters((f) => ({ ...f, minPrice: e.target.value }))} />
        <input className="input" placeholder="Мзда до" type="number" min="0" value={filters.maxPrice}
               onChange={(e) => setFilters((f) => ({ ...f, maxPrice: e.target.value }))} />
        <select className="input" value={filters.minStars}
                onChange={(e) => setFilters((f) => ({ ...f, minStars: e.target.value }))}>
          <option value="">Любые звёзды</option>
          <option value="3">3★ и выше</option>
          <option value="4">4★ и выше</option>
          <option value="5">5★</option>
        </select>
        <button className="btn btn--primary" onClick={search}>Сыскать</button>
      </div>

      {error && <div className="alert alert--error">{error}</div>}

      {loading ? <div className="muted">Подождите…</div> : hotels.length === 0 ? (
        <div className="muted">Дворов не нашлось. Измени параметры поиска.</div>
      ) : (
        <div className="grid">
          {hotels.map(h => (
            <Link key={h.id} to={`/hotels/${h.id}`} className="card hotel">
              <div className="hotel__photo" style={{ background: '#e6efe9' }}>
                {h.photos?.[0] ? <img alt={h.name} src={h.photos[0]} /> : <span className="muted">фото</span>}
              </div>
              <div className="hotel__body">
                <div className="strong">{h.name}</div>
                <div className="muted small">{h.city} • {h.stars ? '★'.repeat(h.stars) : '—'}</div>
                <div className="price">{Number(h.pricePerNight).toLocaleString('ru-RU')} ₽ за ночь</div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
