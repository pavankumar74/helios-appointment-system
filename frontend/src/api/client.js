import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL || '';

const api = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
});

// Attach the JWT (if present) to every request.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('helios_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Normalize error messages coming from the backend's ApiError shape.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const data = error?.response?.data;
    let message = 'Something went wrong. Please try again.';
    if (data?.fieldErrors) {
      message = Object.values(data.fieldErrors).join(' ');
    } else if (data?.message) {
      message = data.message;
    } else if (error.message) {
      message = error.message;
    }
    // Auto-logout on 401 for a smoother UX.
    if (error?.response?.status === 401) {
      localStorage.removeItem('helios_token');
      localStorage.removeItem('helios_user');
    }
    return Promise.reject(new Error(message));
  }
);

export default api;
