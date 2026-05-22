import api from './client';

export async function uploadFile(file, category) {
  const form = new FormData();
  form.append('file', file);
  if (category) form.append('category', category);
  const { data } = await api.post('/files', form);
  return data;
}

export const fileUrl = (id) => `/api/files/${id}`;
