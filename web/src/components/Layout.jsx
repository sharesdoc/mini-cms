import React, { useEffect } from 'react';
import { Outlet, NavLink, Navigate, useLocation } from 'react-router-dom';
import { Icon } from './ui.jsx';
import ProfileNag from './ProfileNag.jsx';
import { useAuth } from '../store/auth.jsx';
import { User } from '../api/client.js';

// 布局：移动端底部 tab，桌面端左侧栏。客户/管理员导航项不同。
export default function Layout() {
  const { isAdmin } = useAuth();
  const loc = useLocation();

  // 每次进入/切换页面都校验 session 有效性。token 过期/失效时，
  // client.js 收到 401 → dispatch 'cms:auth-expired' → AuthProvider 清 state → Protected 自动跳 /login。
  // 覆盖 /me 等 mount 时不主动发请求的页面。
  useEffect(() => { User.me().catch(() => {}); }, [loc.pathname]);

  const customerNav = [
    { to: '/home', icon: 'home', label: '首页' },
    { to: '/customers', icon: 'net', label: '客户' },
    { to: '/messages', icon: 'chat', label: '消息' },
    { to: '/me', icon: 'me', label: '我的' },
  ];
  const adminNav = [
    { to: '/stats', icon: 'stats', label: '报表' },
    { to: '/customers', icon: 'net', label: '客户' },
    { to: '/config', icon: 'cfg', label: '配置' },
    { to: '/me', icon: 'me', label: '我的' },
  ];
  const nav = isAdmin ? adminNav : customerNav;

  // 详情/线程页移动端隐藏底部 tab（沉浸式）
  const immersive = loc.pathname.includes('/customers/') || loc.pathname.includes('/messages/');

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div style={{ fontWeight: 800, fontSize: 18, padding: '4px 14px 18px', color: 'var(--orange)' }}>CMS</div>
        {nav.map((n) => (
          <NavLink key={n.to} to={n.to} className={({ isActive }) => isActive ? 'active' : ''}>
            <Icon name={n.icon} size={20} /> {n.label}
          </NavLink>
        ))}
      </aside>
      <main className="main-col">
        <Outlet />
      </main>
      <ProfileNag />
      {!immersive && (
        <nav className="tabbar">
          {nav.map((n) => (
            <NavLink key={n.to} to={n.to} className={({ isActive }) => isActive ? 'active' : ''}>
              <Icon name={n.icon} size={24} /> <span>{n.label}</span>
            </NavLink>
          ))}
        </nav>
      )}
    </div>
  );
}
