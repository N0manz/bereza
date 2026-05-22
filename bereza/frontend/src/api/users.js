import api from './client';

export const me = () => api.get('/users/me').then(r => r.data);
export const updateMe = (payload) => api.put('/users/me', payload).then(r => r.data);
export const getUser = (id) => api.get(`/users/${id}`).then(r => r.data);
export const searchUsers = (q, page = 0, size = 20) =>
  api.get('/users', { params: { q, page, size } }).then(r => r.data);
