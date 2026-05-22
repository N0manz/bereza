import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getHotel } from '../api/hotels';
import { createBooking } from '../api/bookings';

export default function HotelDetailsPage() {
  const { hotelId } = useParams();
  const [hotel, setHotel] = useState(null);
  const [form, setForm] = useState({ checkIn: '', checkOut: '', guests: 1, rooms: 1 });
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => { getHotel(hotelId).then(setHotel).catch(() => setHotel(null)); }, [hotelId]);

  if (!hotel) return <div className="page"><div className="muted">Подождите…</div></div>;

  const submit = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      const booking = await createBooking({
        hotelId: Number(hotelId),
        checkIn: form.checkIn,
        checkOut: form.checkOut,
        guests: Number(form.guests),
        rooms: Number(form.rooms),
      });
      if (booking.chatId) navigate(`/chats/${booking.chatId}`);
      else navigate('/bookings');
    } catch (err) {
      setError(err?.response?.data?.detail || 'Не удалось забронировать');
    }
  };

  return (
    <div className="page">
      <header className="page__header">
        <h2>{hotel.name}</h2>
        <div className="muted">{hotel.city} • {hotel.stars ? '★'.repeat(hotel.stars) : '—'}</div>
      </header>

      <section className="card">
        <p>{hotel.description}</p>
        <div className="muted small">{hotel.address}</div>
        {hotel.amenities?.length > 0 && (
          <ul className="chips">
            {hotel.amenities.map((a) => <li key={a} className="chip">{a}</li>)}
          </ul>
        )}
        <div className="price big">{Number(hotel.pricePerNight).toLocaleString('ru-RU')} ₽ за ночь</div>
      </section>

      <section className="card">
        <h3>Заездная грамота</h3>
        <form className="form-grid" onSubmit={submit}>
          <label className="field"><span>Заезд</span>
            <input type="date" required value={form.checkIn}
                   onChange={(e) => setForm((f) => ({ ...f, checkIn: e.target.value }))} />
          </label>
          <label className="field"><span>Выезд</span>
            <input type="date" required value={form.checkOut}
                   onChange={(e) => setForm((f) => ({ ...f, checkOut: e.target.value }))} />
          </label>
          <label className="field"><span>Душ</span>
            <input type="number" min="1" max="20" value={form.guests}
                   onChange={(e) => setForm((f) => ({ ...f, guests: e.target.value }))} />
          </label>
          <label className="field"><span>Светлиц</span>
            <input type="number" min="1" max="10" value={form.rooms}
                   onChange={(e) => setForm((f) => ({ ...f, rooms: e.target.value }))} />
          </label>
          {error && <div className="alert alert--error">{error}</div>}
          <button className="btn btn--primary" type="submit">Записать грамоту</button>
        </form>
      </section>
    </div>
  );
}
