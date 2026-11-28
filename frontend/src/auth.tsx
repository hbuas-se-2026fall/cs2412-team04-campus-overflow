import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, ApiError } from './api';
import type { Me } from './types';

interface AuthState {
  me: Me | null;
  loading: boolean;
  /** 审计 2-8：无法确认登录状态（非 401 的失败，如 5xx/网络错），用于避免把临时故障当成「未登录」而跳登录页 */
  authError: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);
  const [authError, setAuthError] = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      setMe(await api.me());
      setAuthError(false);
    } catch (e) {
      // FE-08：只有 401 才表示「未登录/会话失效」；网络抖动、5xx 等临时故障
      // 不应把用户当成登出（否则已登录用户会看起来被突然踢下线）。
      if (e instanceof ApiError && e.status === 401) {
        setMe(null);
        setAuthError(false);
      } else {
        // 审计 2-8：其它错误标记为「状态未知」，保留已有 me，不触发跳登录。
        setAuthError(true);
      }
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
      authError,
      login: async (username, password) => setMe(await api.login(username, password)),
      logout: async () => {
        await api.logout();
        setMe(null);
        setAuthError(false);
      },
      refresh,
    }),
    [me, loading, authError, refresh],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth 必须在 AuthProvider 内使用');
  return ctx;
}
