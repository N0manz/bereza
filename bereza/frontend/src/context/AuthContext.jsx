import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { bootstrapCsrf } from '../api/client';
import { fetchMe, login as apiLogin, logout as apiLogout, register as apiRegister } from '../api/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    try {
      await bootstrapCsrf();
      const me = await fetchMe();
      setUser(me ?? null);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { refresh(); }, [refresh]);

  const login = useCallback(async (username, password) => {
    await bootstrapCsrf();
    const u = await apiLogin(username, password);
    setUser(u);
    return u;
  }, []);

  const logout = useCallback(async () => {
    try { await apiLogout(); } catch {}
    setUser(null);
    await bootstrapCsrf();
  }, []);

  const register = useCallback(async (payload) => {
    await bootstrapCsrf();
    const u = await apiRegister(payload);
    // регистрация не логинит автоматически — фронт логинит после
    return u;
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, register, refresh }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
