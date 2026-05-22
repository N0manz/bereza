const ROLE_LABELS = {
  TOURIST: 'путник',
  GUIDE: 'путевождь',
  HOTEL: 'гостинный двор',
  ADMIN: 'воевода',
};

export function roleLabel(role) {
  if (!role) return '';
  return ROLE_LABELS[role] || role.toLowerCase();
}
