import { useEffect, useRef, useState } from 'react';

const DEFAULT_CENTER = [55.7558, 37.6173]; // Москва
const DEFAULT_ZOOM = 5;

export default function LocationPickerModal({ onClose, onPick, initial }) {
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const [picked, setPicked] = useState(initial || null);
  const [title, setTitle] = useState('');
  const [search, setSearch] = useState('');
  const [searching, setSearching] = useState(false);

  useEffect(() => {
    if (!window.L || !containerRef.current || mapRef.current) return;
    const L = window.L;
    const start = picked ? [picked.lat, picked.lng] : DEFAULT_CENTER;
    const map = L.map(containerRef.current).setView(start, picked ? 13 : DEFAULT_ZOOM);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap',
    }).addTo(map);

    if (picked) {
      markerRef.current = L.marker([picked.lat, picked.lng]).addTo(map);
    }

    map.on('click', (e) => {
      const { lat, lng } = e.latlng;
      setPicked({ lat, lng });
      if (markerRef.current) {
        markerRef.current.setLatLng([lat, lng]);
      } else {
        markerRef.current = L.marker([lat, lng]).addTo(map);
      }
    });

    mapRef.current = map;
    // Leaflet нуждается в invalidateSize, если контейнер появился после mount
    setTimeout(() => map.invalidateSize(), 50);

    return () => {
      map.remove();
      mapRef.current = null;
      markerRef.current = null;
    };
    // eslint-disable-next-line
  }, []);

  const doSearch = async (e) => {
    e?.preventDefault?.();
    const q = search.trim();
    if (!q || !mapRef.current || !window.L) return;
    setSearching(true);
    try {
      const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(q)}`, {
        headers: { 'Accept': 'application/json' },
      });
      const data = await res.json();
      if (data && data[0]) {
        const lat = parseFloat(data[0].lat);
        const lng = parseFloat(data[0].lon);
        mapRef.current.setView([lat, lng], 14);
        setPicked({ lat, lng });
        if (markerRef.current) markerRef.current.setLatLng([lat, lng]);
        else markerRef.current = window.L.marker([lat, lng]).addTo(mapRef.current);
        if (!title.trim() && data[0].display_name) {
          setTitle(data[0].display_name.split(',').slice(0, 2).join(',').trim());
        }
      }
    } catch (err) {
      console.error(err);
    } finally { setSearching(false); }
  };

  const confirm = () => {
    if (!picked) return;
    onPick({ ...picked, title: title.trim() || null });
  };

  return (
    <div className="modal" role="dialog" aria-modal="true" onClick={onClose}>
      <div className="modal__card" onClick={(e) => e.stopPropagation()}
           style={{ maxWidth: 720, width: '100%' }}>
        <h3>Указать место на карте</h3>
        <form onSubmit={doSearch} style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
          <input className="input" placeholder="Поиск по адресу…"
                 value={search} onChange={(e) => setSearch(e.target.value)} />
          <button className="btn btn--ghost" type="submit" disabled={searching}>
            {searching ? '…' : 'Сыскать'}
          </button>
        </form>
        <div ref={containerRef}
             style={{ height: 360, width: '100%', borderRadius: 4, overflow: 'hidden', border: '1px solid var(--border)' }} />
        <div className="muted small" style={{ marginTop: 6 }}>
          {picked
            ? <>Указано: {picked.lat.toFixed(5)}, {picked.lng.toFixed(5)}</>
            : 'Ткни по карте, дабы поставить метку.'}
        </div>
        <label className="field" style={{ marginTop: 8 }}>
          <span>Подпись (по желанию)</span>
          <input className="input" value={title} onChange={(e) => setTitle(e.target.value)}
                 placeholder="Например: место сбора у Кремля" />
        </label>
        <div className="modal__actions">
          <button className="btn btn--ghost" onClick={onClose}>Отступить</button>
          <button className="btn btn--primary" disabled={!picked} onClick={confirm}>
            Послать метку
          </button>
        </div>
      </div>
    </div>
  );
}
