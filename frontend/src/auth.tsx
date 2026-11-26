import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, ApiError } from './api';
import type { Me } from './types';

interface AuthState {
  me: Me | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    try {
      setMe(await api.me());
    } catch (e) {
      // FE-08：只有 401 才表示「未登录/会话失效」；网络抖动、5xx 等临时故障
      // 不应把用户当成登出（否则已登录用户会看起来被突然踢下线）。
      if (e instanceof ApiError && e.status === 401) {
        setMe(null);
      }
      // 其他错误保留当前 me，不覆盖；下次进入页面或手动刷新时再重试。
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    api
      .csrf()
      .catch(() => undefined)
      .then(refresh);
  }, [refresh]);

  const value = useMemo<AuthState>(
    () => ({
      me,
      loading,
      login: async (username, password) => setMe(await api.login(username, password)),
      logout: async () => {
        await api.logout();
        setMe(null);
      },
      refresh,
    }),
    [me, loading, refresh],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth 必须在 AuthProvider 内使用');
  return ctx;
}
