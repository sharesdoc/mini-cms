import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useI18n } from '../i18n/index.jsx';
import { useAuth } from '../store/auth.jsx';
import { User } from '../api/client.js';
import { localizeError } from '../api/client.js';
import { Avatar, useToast, Sheet } from '../components/ui.jsx';

export default function Home() {
  const { t, tpl, levelName } = useI18n();
  const { user } = useAuth();
  const nav = useNavigate();
  const toast = useToast();
  const [home, setHome] = useState(null);
  const [expanding, setExpanding] = useState(false);
  const [targetId, setTargetId] = useState('');
  const [expErr, setExpErr] = useState('');

  const load = async () => {
    try { setHome(await User.home()); } catch (e) { toast(localizeError(e, t)); }
  };
  useEffect(() => { load(); }, []);

  const doExpand = async () => {
    setExpErr('');
    try {
      const r = await User.expand(targetId.trim());
      setExpanding(false); setTargetId('');
      toast(r.upgraded ? tpl(t.okUpgrade, { l: levelName(r.newLevel) }) : tpl(t.okExpand, { n: '' }));
      load();
    } catch (e) { setExpErr(localizeError(e, t)); }
  };

  if (!home) return <div className="page"><div className="card">加载中…</div></div>;
  const badge = (l) => l === 1 ? { bg: '#FFF0E4', fg: '#D9600B' } : l === 2 ? { bg: '#EAF1FF', fg: '#2B5FD9' } : { bg: '#F1F3F6', fg: '#5A616E' };
  const b = badge(home.level);

  return (
    <div className="page">
      <div className="page-header"><div className="page-title">{t.home}</div></div>

      <div className="card" style={{ marginBottom: 16, background: 'linear-gradient(135deg,#FFF3E9,#fff)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 14 }}>
          <Avatar name={user?.name} idx={user?.avatar} size={52} />
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 800, fontSize: 18 }}>{user?.name}</div>
            <span className="badge" style={{ background: b.bg, color: b.fg }}>{levelName(home.level)}</span>
          </div>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          <div style={{ background: '#fff', borderRadius: 12, padding: 12 }}>
            <div style={{ fontSize: 12, color: 'var(--text-mute)' }}>{t.downstream}</div>
            <div style={{ fontSize: 24, fontWeight: 800, color: 'var(--orange)' }}>{home.kidCount}</div>
          </div>
          <div style={{ background: '#fff', borderRadius: 12, padding: 12 }}>
            <div style={{ fontSize: 12, color: 'var(--text-mute)' }}>{t.quotaLeft}</div>
            <div style={{ fontSize: 24, fontWeight: 800 }}>{home.quota}</div>
          </div>
        </div>
      </div>

      {home.canUpgrade ? (
        <div className="card" style={{ marginBottom: 16 }}>
          <div style={{ fontWeight: 700, marginBottom: 8 }}>{t.upgradeProgress}</div>
          <div style={{ height: 8, background: '#F3F4F7', borderRadius: 8, overflow: 'hidden', marginBottom: 8 }}>
            <div style={{ height: '100%', width: `${Math.min(100, ((home.threshold - home.needCount) / home.threshold) * 100)}%`, background: 'linear-gradient(90deg,#FFB347,#FF6A00)' }} />
          </div>
          <div style={{ fontSize: 13, color: 'var(--text-sub)' }}>{tpl(t.upgradeHint2, { n: home.needCount, l: levelName(home.level - 1) })}</div>
        </div>
      ) : (
        <div className="card" style={{ marginBottom: 16, fontSize: 13, color: 'var(--text-mute)' }}>{t.maxLevel}</div>
      )}

      <div className="card" style={{ marginBottom: 16 }}>
        <div style={{ fontWeight: 700, marginBottom: 8 }}>{t.myUpline}</div>
        {home.parent ? (
          <div className="row" onClick={() => nav(`/customers/${home.parent.id}`)}>
            <Avatar name={home.parent.name} idx={home.parent.avatar} size={40} />
            <div><div style={{ fontWeight: 600 }}>{home.parent.name}</div><div style={{ fontSize: 12, color: 'var(--text-mute)' }}>ID {home.parent.id}</div></div>
          </div>
        ) : <div style={{ fontSize: 13, color: 'var(--text-mute)' }}>{t.noUpline}</div>}
      </div>

      <button className="btn btn-primary btn-block" style={{ marginBottom: 16 }} onClick={() => setExpanding(true)} disabled={home.quota <= 0}>
        {t.expandCustomer}
      </button>

      <div className="page-header" style={{ marginTop: 8 }}><div className="page-title" style={{ fontSize: 16 }}>{t.recentDown}</div></div>
      {home.kids && home.kids.length ? home.kids.map((k) => (
        <div className="row" key={k.id} onClick={() => nav(`/customers/${k.id}`)}>
          <Avatar name={k.name} idx={k.avatar} size={42} />
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 600 }}>{k.name}</div>
            <div style={{ fontSize: 12, color: 'var(--text-mute)' }}>ID {k.id} · {levelName(k.level)}</div>
          </div>
          {k.kidCount > 0 && <span style={{ fontSize: 13, color: 'var(--orange)' }}>{k.kidCount} ›</span>}
        </div>
      )) : <div className="card" style={{ textAlign: 'center', color: 'var(--text-mute)', fontSize: 13 }}>{t.emptyDown}</div>}

      {expanding && (
        <Sheet title={t.expandTitle} desc={t.expandDesc} onClose={() => setExpanding(false)}
          footer={<button className="btn btn-primary btn-block" onClick={doExpand}>{t.expandOk}</button>}>
          <div className="field"><input value={targetId} onChange={(e) => setTargetId(e.target.value)} placeholder={t.expandPh} /></div>
          {expErr && <div style={{ color: 'var(--red)', fontSize: 13 }}>{expErr}</div>}
        </Sheet>
      )}
    </div>
  );
}
