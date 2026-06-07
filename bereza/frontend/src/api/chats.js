import api from './client';

export const listChats = (page = 0, size = 30) =>
  api.get('/chats', { params: { page, size } }).then(r => r.data);

export const getChat = (id) => api.get(`/chats/${id}`).then(r => r.data);

export const createChat = (payload) => api.post('/chats', payload).then(r => r.data);

export const listMembers = (chatId) =>
  api.get(`/chats/${chatId}/members`).then(r => r.data);

export const addMember = (chatId, userId) =>
  api.post(`/chats/${chatId}/members`, { userId });

export const removeMember = (chatId, userId) =>
  api.delete(`/chats/${chatId}/members/${userId}`);

export const leaveChat = (chatId) =>
  api.delete(`/chats/${chatId}/leave`);

export const listMessages = (chatId, { before, size = 50 } = {}) =>
  api.get(`/chats/${chatId}/messages`, { params: { before, size } }).then(r => r.data);

export const sendMessage = (chatId, payload) =>
  api.post(`/chats/${chatId}/messages`, payload).then(r => r.data);

export const markRead = (chatId, messageId) =>
  api.post(`/chats/${chatId}/messages/${messageId}/read`);

export const deleteMessage = (chatId, messageId) =>
  api.delete(`/chats/${chatId}/messages/${messageId}`);
