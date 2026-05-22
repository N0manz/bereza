import api from './client';

export const listNotifications = (page = 0, size = 30) =>
  api.get('/notifications', { params: { page, size } }).then(r => r.data);
export const unreadCount = () =>
  api.get('/notifications/unread/count').then(r => r.data.count ?? 0);
export const markNotificationRead = (id) =>
  api.post(`/notifications/${id}/read`);
