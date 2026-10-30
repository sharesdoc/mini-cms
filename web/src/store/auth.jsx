import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { getRole, setToken, setRole, clearSession, Auth, User } from '../api/client.js';

// 空闲页面主动检测间隔：页面停留期间无任何导航/请求也能在此周期内发现登录超时
const IDLE_HEARTBEAT_MS = 60000;

// 认证上下文：保存当前 user/role，提供登录态写入与登出
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('cms_user');
    try { return raw ? JSON.parse(raw) : null; } catch { return null; }
  });
  const [role, setRoleState] = useState(() => getRole());

  const loginSession = useCallback((token, r, u) => {
    setToken(token);
    setRole(r);
    localStorage.setItem('cms_user', JSON.stringify(u));
    setRoleState(r);
    setUser(u);
  }, []);

  const logout = useCallback(async () => {
    try { await Auth.logout(); } catch {}
    clearSession();
    setRoleState('');
    setUser(null);
  }, []);

  const refreshUser = useCallback((u) => {
    localStorage.setItem('cms_user', JSON.stringify(u));
    setUser(u);
  }, []);

  // 监听 token 失效事件（api 401 时 dispatch）：清 React state，
  // Protected 路由随即检测 role 为空 → 自动跳转 /login
  useEffect(() => {
    const onExpired = () => { clearSession(); setRoleState(''); setUser(null); };
    window.addEventListener('cms:auth-expired', onExpired);
    return () => window.removeEventListener('cms:auth-expired', onExpired);
  }, []);

  // 空闲页面主动检测：已登录时定期探活 /api/user/me；
  // 页面完全静止（无导航、无其他请求）时也能在超时后自动触发上面的 401 → auth-expired 跳转链路
  useEffect(() => {
    if (!role) return;
    const timer = setInterval(() => { User.me().catch(() => {}); }, IDLE_HEARTBEAT_MS);
    return () => clearInterval(timer);
  }, [role]);

  return (
    <AuthContext.Provider value={{ user, role, isAdmin: role === 'ADMIN', loginSession, logout, refreshUser, setUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
