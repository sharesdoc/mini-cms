import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useI18n } from '../i18n/index.jsx';
import { useAuth } from '../store/auth.jsx';
import { Customers as CustApi, Admin } from '../api/client.js';
import { localizeError } from '../api/client.js';
import { Avatar, useToast, Sheet } from '../components/ui.jsx';

export default function CustomerDetail() {
  const { id } = useParams();
  const { t, tpl, levelName } = useI18n();
  const { isAdmin } = useAuth();
  const nav = useNavigate();
  const toast = useToast();
  const [d, setD] = useState(null);
  const [quotaVal, setQuotaVal] = useState('');
  const [quotaOpen, setQuotaOpen] = useState(false);
  const [delOpen, setDelOpen] = useState(false);

  const load = async () => {
    try { setD(await CustApi.detail(id)); } catch (e) { toast(localizeError(e, t)); nav(-1); }
  };
  useEffect(() => { load(); }, [id]);

  const refresh = async () => { try { setD(await CustApi.detail(id)); } catch {} };

  const toggleStatus = async () => {
    try { await Admin.setStatus(id, d.user.status === 'active' ? 'disabled' : 'active'); toast(t.okStatus); refresh(); } catch (e) { toast(localizeError(e, t)); }
  };
  const doQuota = async () => {
    try { await Admin.setQuota(id, parseInt(quotaVal, 10)); setQuotaOpen(false); toast(tpl(t.okQuota, { n: quotaVal })); refresh(); } catch (e) { toast(localizeError(e, t)); }
  };
  const doDelete = async () => {
    try { await Admin.remove(id); setDelOpen(false); toast(t.okDel); nav(-1); } catch (e) { toast(localizeError(e, t)); }
  };

  if (!d) return <div className="page"><div className="card">加载中…</div></div>;
  const u = d.user;
  const stt = u.status === 'active' ? { bg: '#EAF7EF', fg: '#127A3E', text: t.active } : u.status === 'disabled' ? { bg: '#FFF2F0', fg: '#C7361B', text: t.disabled } : { bg: '#F1F3F6', fg: '#8A909E', text: t.deleted };
  const na = { color: '#C3C8D2' };
  const fdate = (ts) => ts ? ts.slice(0, 10) : '—';
  const rows = [
    { k: t.userId, v: u.id },
    { k: t.account, v: u.username },
    { k: t.realName, v: u.name },
    u.idcard ? { k: t.idCard, v: u.idcard } : { k: t.idCard, v: t.notFilled, ...na },
    u.phone ? { k: t.phone, v: u.phone } : { k: t.phone, v: t.notFilled, ...na },
    u.address ? { k: t.address, v: u.address } : { k: t.address, v: t.notFilled, ...na },
    { k: t.level, v: levelName(u.level) },
    { k: t.upline, v: u.parentId || '—' },
    { k: t.joinedAt, v: fdate(u.createdAt) },
  ];

  return (
    <div className="page">
      {/* 顶部返回 */}
      <div className="page-header">
        <button onClick={() => nav(-1)} className="btn btn-ghost" style={{ padding: '8px 12px' }}>‹</button>
        <div className="page-title">{t.profileInfo}</div>
      </div>

      <div className="card" style={{ textAlign: 'center', marginBottom: 16 }}>
        <Avatar name={u.name} idx={u.avatar} size={64} />
        <div style={{ fontWeight: 800, fontSize: 18, marginTop: 10 }}>{u.name}</div>
        <span className="badge" style={{ background: stt.bg, color: stt.fg, marginTop: 6, display: 'inline-block' }}>{stt.text}</span>
      </div>

      {/* 资料行 */}
      <div className="card" style={{ marginBottom: 16 }}>
        {rows.map((r, i) => (
          <div key={i} style={{ display: 'flex', justifyContent: 'space-between', padding: '9px 0', borderBottom: i < rows.length - 1 ? '1px solid var(--border)' : 'none' }}>
            <span style={{ fontSize: 13, color: 'var(--text-mute)' }}>{r.k}</span>
            <span style={{ fontSize: 13, fontWeight: 600, color: r.color || 'var(--text)', maxWidth: '60%', textAlign: 'right', wordBreak: 'break-all' }}>{r.v}</span>
          </div>
        ))}
      </div>

      {/* 管理员操作 */}
      {isAdmin && (
        <div className="card" style={{ marginBottom: 16 }}>
          <div style={{ fontWeight: 700, marginBottom: 12 }}>{t.adminActions}</div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <button className="btn btn-ghost" onClick={() => setQuotaOpen(true)}>{t.adjustQuota}</button>
            <button className="btn btn-ghost" onClick={toggleStatus} style={{ color: u.status === 'active' ? 'var(--red)' : 'var(--green)' }}>{u.status === 'active' ? t.disable : t.activate}</button>
            <button className="btn btn-ghost" onClick={() => setDelOpen(true)} style={{ color: 'var(--red)', gridColumn: '1/3' }}>{t.softDelete}</button>
          </div>
        </div>
      )}

      {/* 其下游 */}
      <div className="page-header"><div className="page-title" style={{ fontSize: 16 }}>{t.itsDownstream} · {d.kids.length}</div></div>
      {d.kids.length ? d.kids.map((k) => (
        <div className="row" key={k.id} onClick={() => nav(`/customers/${k.id}`)}>
          <Avatar name={k.name} idx={k.avatar} size={40} />
          <div style={{ flex: 1 }}><div style={{ fontWeight: 600 }}>{k.name}</div><div style={{ fontSize: 12, color: 'var(--text-mute)' }}>ID {k.id} · {levelName(k.level)}</div></div>
          {k.kidCount > 0 && <span style={{ fontSize: 13, color: 'var(--orange)' }}>{k.kidCount} ›</span>}
        </div>
      )) : <div className="card" style={{ textAlign: 'center', color: 'var(--text-mute)', fontSize: 13 }}>{t.emptyLeaf}</div>}

      {quotaOpen && (
        <Sheet title={t.quotaTitle} desc={t.quotaDesc} onClose={() => setQuotaOpen(false)}
          footer={<button className="btn btn-primary btn-block" onClick={doQuota}>{t.quotaOk}</button>}>
          <div className="field"><input value={quotaVal} onChange={(e) => setQuotaVal(e.target.value)} placeholder={t.quotaPh} type="number" /></div>
        </Sheet>
      )}
      {delOpen && (
        <Sheet title={t.delTitle} desc={t.delDesc} onClose={() => setDelOpen(false)}
          footer={<div style={{ display: 'flex', gap: 10 }}><button className="btn btn-ghost btn-block" onClick={() => setDelOpen(false)}>{t.cancel}</button><button className="btn btn-primary btn-block" onClick={doDelete} style={{ background: 'var(--red)' }}>{t.delOk}</button></div>}>
        </Sheet>
      )}
    </div>
  );
}
