import api from './client';

export const createGeoPoint = (payload) => api.post('/geo/points', payload).then(r => r.data);
export const geoForChat = (chatId) => api.get(`/geo/chats/${chatId}`).then(r => r.data);
