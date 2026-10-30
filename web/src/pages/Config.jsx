import React, { useState, useEffect } from 'react';
import { useI18n } from '../i18n/index.jsx';
import { Admin } from '../api/client.js';
import { localizeError } from '../api/client.js';
import { useToast } from '../components/ui.jsx';

export default function Config() {
  const { t, tpl, levelName } = useI18n();
  const toast = useToast();
  const [cfg, setCfg] = useState(null);
  const [quota, setQuota] = useState({});
  const [upgrade, setUpgrade] = useState({});

  const load = async () => {
    try {
      const c = await Admin.config();
      setCfg(c);
      setQuota(c.quota || {});
      setUpgrade(c.upgrade || {});
    } catch (e) { toast(localizeError(e, t)); }
  };
  useEffect(() => { load(); }, []);

  const save = async () => {
    try { await Admin.updateConfig(quota, upgrade); toast(t.okSave); load(); } catch (e) { toast(localizeError(e, t)); }
  };
  const bump = (obj, set, k, d) => set({ ...obj, [k]: Math.max(0, (obj[k] || 0) + d) });

  if (!cfg) return <div className="page"><div className="card">加载中…</div></div>;
  const max = cfg.maxLevel;

  return (
    <div className="page">
      <div className="page-header"><div className="page-title">{t.config}</div></div>

      {/* 部署配置：级别数（只读） */}
      <div className="card" style={{ marginBottom: 16 }}>
        <div style={{ fontWeight: 700, marginBottom: 10 }}>{t.deployConfig}</div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <span style={{ fontSize: 14 }}>{t.levelsSupported}</span>
          <span className="badge" style={{ background: 'var(--orange-bg)', color: 'var(--orange)', fontSize: 16, padding: '4px 14px' }}>{max}</span>
        </div>
        <div style={{ fontSize: 12, color: 'var(--text-mute)', lineHeight: 1.6, background: '#FFF8F2', padding: 10, borderRadius: 10 }}>🔒 {t.cfgLock}</div>
      </div>

      {/* 各级默认指标 */}
      <div className="card" style={{ marginBottom: 16 }}>
        <div style={{ fontWeight: 700, marginBottom: 10 }}>{t.quotaConfig}</div>
        {Object.keys(quota).sort().map((k) => (
          <div key={'q' + k} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
            <div><div style={{ fontWeight: 600, fontSize: 14 }}>{levelName(parseInt(k, 10))}</div><div style={{ fontSize: 11, color: 'var(--text-mute)' }}>{t.quotaNote}</div></div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <button className="btn btn-ghost" style={{ padding: '4px 12px' }} onClick={() => bump(quota, setQuota, k, -1)}>−</button>
              <span style={{ fontWeight: 800, minWidth: 24, textAlign: 'center' }}>{quota[k]}</span>
              <button className="btn btn-ghost" style={{ padding: '4px 12px' }} onClick={() => bump(quota, setQuota, k, 1)}>+</button>
            </div>
          </div>
        ))}
      </div>

      {/* 升级门槛 */}
      <div className="card" style={{ marginBottom: 16 }}>
        <div style={{ fontWeight: 700, marginBottom: 10 }}>{t.upgradeConfig}</div>
        {Object.keys(upgrade).sort().map((k) => (
          <div key={'u' + k} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
            <div><div style={{ fontWeight: 600, fontSize: 14 }}>{levelName(parseInt(k, 10))}</div><div style={{ fontSize: 11, color: 'var(--text-mute)' }}>{tpl(t.upNote, { n: upgrade[k], l: levelName(parseInt(k, 10) - 1) })}</div></div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <button className="btn btn-ghost" style={{ padding: '4px 12px' }} onClick={() => bump(upgrade, setUpgrade, k, -1)}>−</button>
              <span style={{ fontWeight: 800, minWidth: 24, textAlign: 'center' }}>{upgrade[k]}</span>
              <button className="btn btn-ghost" style={{ padding: '4px 12px' }} onClick={() => bump(upgrade, setUpgrade, k, 1)}>+</button>
            </div>
          </div>
        ))}
      </div>

      <button className="btn btn-primary btn-block" onClick={save}>{t.save}</button>
    </div>
  );
}
