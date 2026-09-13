export function apiError(error, fallback) {
  const data = error.response?.data;
  if (typeof data === 'string' && data) return data;
  if (data?.message) return data.message;
  if (data && typeof data === 'object') return Object.values(data).join('. ');
  return fallback;
}
