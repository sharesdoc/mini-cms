import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useI18n } from '../i18n/index.jsx';
import { Admin } from '../api/client.js';
import { localizeError } from '../api/client.js';
import { Avatar, useToast } from '../components/ui.jsx';

export default function Stats() {
  const { t, levelName } = useI18n();
  const nav = useNavigate();
  const toast = useToast();
  const [s, setS] = useState(null);

  useEffect(() => {
    Admin.stats().then(setS).catch((e) => toast(localizeError(e, t)));
  }, []);

  if (!s) return <div className="page"><div className="card">加载中…</div></div>;

  const kpis = [
    { label: t.totalUsers, value: s.total, sub: t.allTime, color: 'var(--text)' },
    { label: t.todayNew, value: s.todayNew, sub: t.vsYesterday, color: 'var(--orange)' },
    { label: t.weekNew, value: s.weekNew, sub: t.last7, color: 'var(--blue)' },
    { label: t.disabledCount, value: s.disabledCount, sub: t.needAttention, color: 'var(--red)' },
  ];
  const maxN = Math.max(1, ...s.series7.map((p) => p.count));

  return (
    <div className="page">
      <div className="page-header"><div className="page-title">{t.stats}</div></div>

      {/* KPI */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 16 }}>
        {kpis.map((k, i) => (
          <div key={i} className="card" style={{ padding: 14 }}>
            <div style={{ fontSize: 12, color: 'var(--text-mute)' }}>{k.label}</div>
            <div style={{ fontSize: 28, fontWeight: 800, color: k.color }}>{k.value}</div>
            <div style={{ fontSize: 11, color: 'var(--text-mute)' }}>{k.sub}</div>
          </div>
        ))}
      </div>

      {/* 7 日柱状 */}
      <div className="card" style={{ marginBottom: 16 }}>
        <div style={{ fontWeight: 700, marginBottom: 14 }}>{t.newUsers7}</div>
        <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8, height: 120 }}>
          {s.series7.map((p, i) => (
            <div key={i} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
              <div style={{ fontSize: 11, fontWeight: 700, color: i === s.series7.length - 1 ? 'var(--orange)' : 'var(--text-sub)' }}>{p.count}</div>
              <div style={{ width: '100%', maxWidth: 28, height: Math.max(4, (p.count / maxN) * 80), borderRadius: '6px 6px 2px 2px', background: i === s.series7.length - 1 ? 'linear-gradient(180deg,#FF9138,#FF6A00)' : 'linear-gradient(180deg,#FFD6B4,#FFB27A)' }} />
              <div style={{ fontSize: 10, color: 'var(--text-mute)' }}>{p.date}</div>
            </div>
          ))}
        </div>
      </div>

      {/* 级别分布 */}
      <div className="card" style={{ marginBottom: 16 }}>
        <div style={{ fontWeight: 700, marginBottom: 12 }}>{t.levelDist}</div>
        {s.levelDist.map((l) => (
          <div key={l.level} style={{ marginBottom: 10 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 4 }}>
              <span>{levelName(l.level)}</span><span style={{ color: 'var(--text-mute)' }}>{l.count} · {l.pct}%</span>
            </div>
            <div style={{ height: 8, background: '#F3F4F7', borderRadius: 8, overflow: 'hidden' }}>
              <div style={{ height: '100%', width: `${l.pct}%`, background: l.level === 1 ? 'linear-gradient(90deg,#FFB347,#FF6A00)' : l.level === 2 ? 'linear-gradient(90deg,#8FB6FF,#2B5FD9)' : 'linear-gradient(90deg,#C9CFDA,#8A93A5)' }} />
            </div>
          </div>
        ))}
      </div>

      {/* 最近注册 */}
      <div className="page-header"><div className="page-title" style={{ fontSize: 16 }}>{t.recentReg}</div></div>
      {s.recent.map((r) => (
        <div className="row" key={r.id} onClick={() => nav(`/customers/${r.id}`)}>
          <Avatar name={r.name} idx={r.avatar} size={40} />
          <div style={{ flex: 1 }}><div style={{ fontWeight: 600 }}>{r.name}</div><div style={{ fontSize: 12, color: 'var(--text-mute)' }}>ID {r.id} · {levelName(r.level)}</div></div>
        </div>
      ))}
    </div>
  );
}
