import { useEffect, useRef } from 'react';

function MessageBody({ m, fileUrl }) {
  if (m.deleted) return <em className="muted">Слово стёрто</em>;
  if (m.type === 'GEO' && m.payload?.latitude) {
    const { latitude, longitude } = m.payload;
    const yandexUrl = `https://yandex.ru/maps/?ll=${longitude},${latitude}&pt=${longitude},${latitude}&z=15`;
    return (
      <div>
        <div>{m.content}</div>
        <a className="link" href={yandexUrl} target="_blank" rel="noreferrer">
          Открыть на карте
        </a>
      </div>
    );
  }
  if (m.type === 'BOOKING' && m.payload) {
    const p = m.payload;
    return (
      <div className="booking-card">
        <div className="strong">Грамота №{p.bookingId}</div>
        <div>{p.hotelName}</div>
        <div className="muted small">{p.checkIn} → {p.checkOut}</div>
        {(p.guests != null || p.rooms != null) && (
          <div className="muted small">
            {p.guests != null && <>Душ: {p.guests}</>}
            {p.guests != null && p.rooms != null && ' • '}
            {p.rooms != null && <>Светлиц: {p.rooms}</>}
          </div>
        )}
        <div className="price">{Number(p.totalPrice).toLocaleString('ru-RU')} ₽</div>
      </div>
    );
  }
  if (m.type === 'SYSTEM') return <em className="muted">{m.content}</em>;

  return (
    <>
      {m.content && <div className="msg__text">{m.content}</div>}
      {m.attachments?.length > 0 && (
        <ul className="msg__attachments">
          {m.attachments.map(a => (
            <li key={a.id}>
              {a.mimeType?.startsWith('image/')
                ? <img src={fileUrl(a.id)} alt={a.originalName} className="msg__image" />
                : <a className="link" href={fileUrl(a.id)} target="_blank" rel="noreferrer">📎 {a.originalName}</a>}
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

export default function MessageList({ messages, currentUserId, fileUrl }) {
  const ref = useRef(null);

  useEffect(() => {
    if (ref.current) ref.current.scrollTop = ref.current.scrollHeight;
  }, [messages]);

  return (
    <div className="messages" ref={ref}>
      {messages.map(m => {
        const isOwn = m.senderId === currentUserId;
        const cls = m.type === 'SYSTEM' ? 'msg msg--system'
          : (isOwn ? 'msg msg--own' : 'msg');
        return (
          <div key={m.id} className={cls}>
            {!isOwn && m.type !== 'SYSTEM' && (
              <div className="msg__author">{m.senderDisplayName}</div>
            )}
            <div className="msg__bubble">
              <MessageBody m={m} fileUrl={fileUrl} />
            </div>
            <div className="msg__meta">
              {new Date(m.createdAt).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })}
              {isOwn && m.readBy?.length > 0 && <span title="Прочитано"> ✓✓</span>}
            </div>
          </div>
        );
      })}
    </div>
  );
}
