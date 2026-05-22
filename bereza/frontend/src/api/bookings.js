import api from './client';

export const createBooking = (payload) => api.post('/bookings', payload).then(r => r.data);
export const myBookings = (page = 0, size = 20) =>
  api.get('/bookings/my', { params: { page, size } }).then(r => r.data);
export const incomingBookings = (page = 0, size = 20) =>
  api.get('/bookings/incoming', { params: { page, size } }).then(r => r.data);
export const changeStatus = (id, status) =>
  api.post(`/bookings/${id}/status`, { status }).then(r => r.data);
