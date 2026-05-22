import api from './client';

export const searchHotels = (params = {}) =>
  api.get('/hotels', { params }).then(r => r.data);

export const getHotel = (id) => api.get(`/hotels/${id}`).then(r => r.data);

export const myHotels = () => api.get('/hotels/my').then(r => r.data);

export const createHotel = (payload) => api.post('/hotels', payload).then(r => r.data);
export const updateHotel = (id, payload) => api.put(`/hotels/${id}`, payload).then(r => r.data);
