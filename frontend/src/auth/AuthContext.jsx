import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import api from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('helios_user');
    return raw ? JSON.parse(raw) : null;
  });
  const [token, setToken] = useState(() => localStorage.getItem('helios_token'));

  useEffect(() => {
    if (token) localStorage.setItem('helios_token', token);
    else localStorage.removeItem('helios_token');
  }, [token]);

  useEffect(() => {
    if (user) localStorage.setItem('helios_user', JSON.stringify(user));
    else localStorage.removeItem('helios_user');
  }, [user]);

  const applyAuth = (data) => {
    setToken(data.token);
    setUser({ id: data.userId, name: data.name, email: data.email, role: data.role });
  };

  const login = async (email, password) => {
    const { data } = await api.post('/api/auth/login', { email, password });
    applyAuth(data);
    return data;
  };

  const register = async (payload) => {
    const { data } = await api.post('/api/auth/register', payload);
    applyAuth(data);
    return data;
  };

  const logout = () => {
    setToken(null);
    setUser(null);
  };

  const value = useMemo(
    () => ({ user, token, isAuthenticated: !!token, login, register, logout }),
    [user, token]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
