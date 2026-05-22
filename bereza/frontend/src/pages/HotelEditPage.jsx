import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { createHotel, getHotel, updateHotel } from '../api/hotels';
import { uploadFile, fileUrl } from '../api/files';

const PRESET_AMENITIES = [
  'Wi-Fi', 'Завтрак', 'Парковка', 'Спа', 'Баня',
  'Бассейн', 'Тренажёрный зал', 'Ресторан', 'Бар',
  'Кондиционер', 'Прачечная', 'Сейф', 'Трансфер',
];

const EMPTY_FORM = {
  name: '', description: '', city: '', address: '',
  stars: '', pricePerNight: '', currency: 'RUB', roomsAvailable: 1,
  photos: [], amenities: [], active: true,
};

export default function HotelEditPage() {
  const { hotelId } = useParams();
  const navigate = useNavigate();
  const isEdit = !!hotelId;

  const [form, setForm] = useState(EMPTY_FORM);
  const [amenityInput, setAmenityInput] = useState('');
  const [loading, setLoading] = useState(isEdit);
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!isEdit) return;
    let alive = true;
    getHotel(hotelId)
      .then((h) => {
        if (!alive) return;
        setForm({
          name: h.name || '',
          description: h.description || '',
          city: h.city || '',
          address: h.address || '',
          stars: h.stars ?? '',
          pricePerNight: h.pricePerNight ?? '',
          currency: h.currency || 'RUB',
          roomsAvailable: h.roomsAvailable ?? 0,
          photos: h.photos || [],
          amenities: h.amenities || [],
          active: h.active ?? true,
        });
      })
      .catch((e) => setError(e?.response?.data?.detail || 'Не удалось загрузить отель'))
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [hotelId, isEdit]);

  const setField = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const toggleAmenity = (a) => setForm((f) => ({
    ...f,
    amenities: f.amenities.includes(a) ? f.amenities.filter(x => x !== a) : [...f.amenities, a],
  }));

  const addCustomAmenity = () => {
    const v = amenityInput.trim();
    if (!v) return;
    if (!form.amenities.includes(v)) {
      setForm((f) => ({ ...f, amenities: [...f.amenities, v] }));
    }
    setAmenityInput('');
  };

  const removePhoto = (idx) => setForm((f) => ({
    ...f, photos: f.photos.filter((_, i) => i !== idx),
  }));

  const onFilesPicked = async (e) => {
    const files = [...e.target.files];
    e.target.value = '';
    if (files.length === 0) return;
    setUploading(true);
    setError(null);
    try {
      for (const f of files) {
        const att = await uploadFile(f, 'IMAGE');
        const url = att.downloadUrl || fileUrl(att.id);
        setForm((prev) => ({ ...prev, photos: [...prev.photos, url] }));
      }
    } catch (err) {
      console.error(err);
      setError('Не удалось загрузить файл');
    } finally { setUploading(false); }
  };

  const submit = async (e) => {
    e.preventDefault();
    setError(null);
    setSaving(true);
    try {
      const payload = {
        name: form.name.trim(),
        description: form.description?.trim() || null,
        city: form.city.trim(),
        address: form.address.trim(),
        stars: form.stars === '' ? null : Number(form.stars),
        pricePerNight: form.pricePerNight === '' ? null : form.pricePerNight,
        currency: form.currency || 'RUB',
        roomsAvailable: Number(form.roomsAvailable) || 0,
        photos: form.photos,
        amenities: form.amenities,
        active: !!form.active,
      };
      if (isEdit) await updateHotel(hotelId, payload);
      else await createHotel(payload);
      navigate('/hotels/manage');
    } catch (err) {
      console.error(err);
      setError(err?.response?.data?.detail
        || (err?.response?.data?.errors && Object.values(err.response.data.errors).join(', '))
        || 'Не удалось сохранить отель');
    } finally { setSaving(false); }
  };

  if (loading) return <div className="page"><div className="muted">Подождите…</div></div>;

  return (
    <div className="page page--narrow">
      <header className="page__header">
        <h2>{isEdit ? 'Переписать двор' : 'Возвести двор'}</h2>
        <button type="button" className="btn btn--ghost" onClick={() => navigate('/hotels/manage')}>
          Назад
        </button>
      </header>

      <form className="card" onSubmit={submit}>
        <div className="form-grid">
          <label className="field" style={{ gridColumn: '1 / -1' }}>
            <span>Имя двора *</span>
            <input className="input" required maxLength={200}
                   value={form.name} onChange={(e) => setField('name', e.target.value)} />
          </label>

          <label className="field" style={{ gridColumn: '1 / -1' }}>
            <span>Сказание о дворе</span>
            <textarea className="input" rows={3}
                      value={form.description}
                      onChange={(e) => setField('description', e.target.value)} />
          </label>

          <label className="field">
            <span>Град *</span>
            <input className="input" required maxLength={120}
                   value={form.city} onChange={(e) => setField('city', e.target.value)} />
          </label>

          <label className="field">
            <span>Улица *</span>
            <input className="input" required maxLength={500}
                   value={form.address} onChange={(e) => setField('address', e.target.value)} />
          </label>

          <label className="field">
            <span>Звёзды</span>
            <select className="input" value={form.stars}
                    onChange={(e) => setField('stars', e.target.value)}>
              <option value="">—</option>
              <option value="1">1★</option>
              <option value="2">2★</option>
              <option value="3">3★</option>
              <option value="4">4★</option>
              <option value="5">5★</option>
            </select>
          </label>

          <label className="field">
            <span>Мзда за ночь (₽) *</span>
            <input className="input" type="number" min="0" step="0.01" required
                   value={form.pricePerNight}
                   onChange={(e) => setField('pricePerNight', e.target.value)} />
          </label>

          <label className="field">
            <span>Светлиц во дворе</span>
            <input className="input" type="number" min="0"
                   value={form.roomsAvailable}
                   onChange={(e) => setField('roomsAvailable', e.target.value)} />
          </label>

          <label className="field">
            <span>Монета</span>
            <select className="input" value={form.currency}
                    onChange={(e) => setField('currency', e.target.value)}>
              <option value="RUB">RUB</option>
              <option value="USD">USD</option>
              <option value="EUR">EUR</option>
            </select>
          </label>

          <div className="field" style={{ gridColumn: '1 / -1' }}>
            <span>Удобства</span>
            <ul className="chips">
              {PRESET_AMENITIES.map(a => {
                const on = form.amenities.includes(a);
                return (
                  <li key={a}
                      className="chip"
                      style={{
                        cursor: 'pointer',
                        background: on ? 'var(--primary)' : 'var(--bg)',
                        color: on ? '#fff' : 'inherit',
                        borderColor: on ? 'var(--primary)' : 'var(--border)',
                      }}
                      onClick={() => toggleAmenity(a)}>
                    {on ? '✓ ' : ''}{a}
                  </li>
                );
              })}
            </ul>
            {form.amenities.filter(a => !PRESET_AMENITIES.includes(a)).length > 0 && (
              <ul className="chips">
                {form.amenities.filter(a => !PRESET_AMENITIES.includes(a)).map(a => (
                  <li key={a} className="chip"
                      style={{ cursor: 'pointer', background: 'var(--primary)', color: '#fff', borderColor: 'var(--primary)' }}
                      onClick={() => toggleAmenity(a)}>
                    ✓ {a} ✕
                  </li>
                ))}
              </ul>
            )}
            <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
              <input className="input" placeholder="Свой узор…" value={amenityInput}
                     onChange={(e) => setAmenityInput(e.target.value)}
                     onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); addCustomAmenity(); } }} />
              <button type="button" className="btn btn--ghost" onClick={addCustomAmenity}>Прибавить</button>
            </div>
          </div>

          <div className="field" style={{ gridColumn: '1 / -1' }}>
            <span>Изображения двора</span>
            {form.photos.length > 0 && (
              <ul className="chips" style={{ marginBottom: 8 }}>
                {form.photos.map((p, i) => (
                  <li key={p + i} className="chip"
                      style={{ display: 'flex', alignItems: 'center', gap: 6, padding: 4 }}>
                    <img src={p} alt="" style={{ width: 48, height: 48, objectFit: 'cover', borderRadius: 4 }} />
                    <button type="button" className="btn btn--ghost btn--sm" onClick={() => removePhoto(i)}>✕</button>
                  </li>
                ))}
              </ul>
            )}
            <input type="file" accept="image/*" multiple onChange={onFilesPicked} disabled={uploading} />
            {uploading && <div className="muted small">Прицепляем…</div>}
          </div>

          <label className="field" style={{ gridColumn: '1 / -1', flexDirection: 'row', alignItems: 'center', gap: 8 }}>
            <input type="checkbox" checked={!!form.active}
                   onChange={(e) => setField('active', e.target.checked)} />
            <span>Отворить врата для путников</span>
          </label>

          {error && <div className="alert alert--error">{error}</div>}

          <div className="modal__actions" style={{ gridColumn: '1 / -1' }}>
            <button type="button" className="btn btn--ghost"
                    onClick={() => navigate('/hotels/manage')} disabled={saving}>Отступить</button>
            <button type="submit" className="btn btn--primary" disabled={saving || uploading}>
              {saving ? 'Записываем…' : (isEdit ? 'Сберечь' : 'Возвести двор')}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}
