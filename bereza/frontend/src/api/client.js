import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  withCredentials: true, // отправляем session-cookie всегда
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
});

// Получить CSRF-токен заранее (cookie XSRF-TOKEN установится автоматически).
export async function bootstrapCsrf() {
  await api.get('/auth/csrf').catch(() => {});
}

export default api;
