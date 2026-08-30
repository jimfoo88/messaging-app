export async function request(path, { token, onUnauthorized, ...options } = {}) {
  const headers = new Headers(options.headers);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  const response = await fetch(path, { ...options, headers });
  if (response.status === 401 || response.status === 403) {
    onUnauthorized?.();
    throw new Error('Your session has ended. Please sign in again.');
  }
  if (!response.ok) throw new Error('Request could not be completed.');
  return response.status === 204 ? null : response.json();
}
