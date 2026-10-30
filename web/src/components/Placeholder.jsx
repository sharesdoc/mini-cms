import React from 'react';
import { useI18n } from '../i18n/index.jsx';

// 占位页：标记该页面待后续迭代实现（不调用未就绪的 API，避免崩溃）
export default function Placeholder({ name }) {
  const { t } = useI18n();
  return (
    <div className="page">
      <div className="page-header"><div className="page-title">{name}</div></div>
      <div className="card" style={{ textAlign: 'center', padding: 40, color: 'var(--text-mute)' }}>
        <div style={{ fontSize: 40, marginBottom: 10 }}>🚧</div>
        <div style={{ fontWeight: 700, marginBottom: 6 }}>{name}</div>
        <div style={{ fontSize: 13 }}>{t.appName} · 该页面将在后续迭代实现</div>
      </div>
    </div>
  );
}
