import api from './client';

export async function login(username, password) {
  const form = new URLSearchParams();
  form.append('username', username);
  form.append('password', password);
  const { data } = await api.post('/auth/login', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  });
  return data;
}

export async function logout() {
  await api.post('/auth/logout');
}

export async function register(payload) {
  const { data } = await api.post('/auth/register', payload);
  return data;
}

export async function fetchMe() {
  const { data } = await api.get('/auth/me');
  return data;
}
