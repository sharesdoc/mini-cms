import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';

// 8 色头像渐变（对齐设计原型）
export const PAL = [
  'linear-gradient(140deg,#FFB05C,#FF6A00)',
  'linear-gradient(140deg,#7BA7FF,#2B5FD9)',
  'linear-gradient(140deg,#6FD6C0,#0E9F85)',
  'linear-gradient(140deg,#FF9AA8,#E8456B)',
  'linear-gradient(140deg,#B79BFF,#6C43E0)',
  'linear-gradient(140deg,#FFD166,#DFA100)',
  'linear-gradient(140deg,#8FA0B8,#43506B)',
  'linear-gradient(140deg,#5EC6FF,#0B7FD1)',
];

// 图标 path（对齐设计原型 ICON）
export const ICON = {
  home: 'M4 11.2 12 4.2l8 7V19a1.2 1.2 0 0 1-1.2 1.2h-3.6v-6H8.8v6H5.2A1.2 1.2 0 0 1 4 19z',
  net: 'M9 11.2a3.4 3.4 0 1 0 0-6.8 3.4 3.4 0 0 0 0 6.8ZM2.6 20.2c0-3.2 2.9-5.4 6.4-5.4s6.4 2.2 6.4 5.4M17 12.4a3 3 0 1 0 0-6M18.6 20.2c0-2-.7-3.6-1.9-4.7',
  chat: 'M20 12.4c0 3.5-3.6 6.4-8 6.4-.9 0-1.8-.1-2.6-.4L5 20.2l1.2-3.1C4.8 15.9 4 14.3 4 12.4 4 8.9 7.6 6 12 6s8 2.9 8 6.4Z',
  stats: 'M5 20V10.5M12 20V4.5M19 20v-6.5',
  me: 'M12 12.2a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4.4 20.4c0-3.6 3.4-6 7.6-6s7.6 2.4 7.6 6',
  cfg: 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm8-3a8 8 0 0 0-.2-1.7l2-1.5-2-3.4-2.3 1a8 8 0 0 0-3-1.7L14.2 2H9.8l-.3 2.7a8 8 0 0 0-3 1.7l-2.3-1-2 3.4 2 1.5a8 8 0 0 0 0 3.4l-2 1.5 2 3.4 2.3-1a8 8 0 0 0 3 1.7l.3 2.7h4.4l.3-2.7a8 8 0 0 0 3-1.7l2.3 1 2-3.4-2-1.5c.1-.5.2-1.1.2-1.7Z',
};

export function Icon({ name, size = 24, color = 'currentColor', sw = 1.8 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={sw} strokeLinecap="round" strokeLinejoin="round">
      <path d={ICON[name] || ''} />
    </svg>
  );
}

export function Avatar({ name, idx = 0, size = 44 }) {
  const initial = (name || '?').trim().charAt(0).toUpperCase();
  return (
    <div className="avatar" style={{ width: size, height: size, background: PAL[(idx || 0) % 8], fontSize: size * 0.42 }}>{initial}</div>
  );
}

// ---- Toast ----
const ToastCtx = createContext(null);
export function ToastProvider({ children }) {
  const [msg, setMsg] = useState('');
  const show = useCallback((m) => {
    setMsg(m);
    setTimeout(() => setMsg(''), 2400);
  }, []);
  return (
    <ToastCtx.Provider value={show}>
      {children}
      {msg && <div className="toast">{msg}</div>}
    </ToastCtx.Provider>
  );
}
export function useToast() {
  const ctx = useContext(ToastCtx);
  return ctx || (() => {});
}

// ---- 底部弹层 Sheet ----
export function Sheet({ title, desc, onClose, children, footer }) {
  useEffect(() => {
    const h = (e) => e.key === 'Escape' && onClose && onClose();
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [onClose]);
  return (
    <div className="mask" onClick={onClose}>
      <div className="sheet" onClick={(e) => e.stopPropagation()}>
        {title && <div style={{ fontSize: 18, fontWeight: 800, marginBottom: 6 }}>{title}</div>}
        {desc && <div style={{ fontSize: 13, color: 'var(--text-mute)', marginBottom: 14, lineHeight: 1.5 }}>{desc}</div>}
        {children}
        {footer && <div style={{ marginTop: 16 }}>{footer}</div>}
      </div>
    </div>
  );
}

export function Spinner() {
  return <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-mute)' }}>加载中…</div>;
}
