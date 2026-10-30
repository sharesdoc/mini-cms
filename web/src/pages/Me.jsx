import React, { useState, useEffect } from 'react';
import { useI18n } from '../i18n/index.jsx';
import { useAuth } from '../store/auth.jsx';
import { User } from '../api/client.js';
import { ApiError } from '../api/client.js';
import { Avatar, useToast, Sheet, PAL } from '../components/ui.jsx';

export default function Me() {
  const { t, lang, setLang, levelName } = useI18n();
  const { user, refreshUser, logout, isAdmin } = useAuth();
  const toast = useToast();
  const [form, setForm] = useState({ name: '', idcard: '', phone: '', address: '' });
  const [pickingAvatar, setPickingAvatar] = useState(false);

  useEffect(() => {
    if (user) setForm({ name: user.name || '', idcard: user.idcard || '', phone: user.phone || '', address: user.address || '' });
  }, [user]);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const reload = async () => {
    try { const u = await User.me(); refreshUser(u); } catch {}
  };

  const save = async () => {
    try {
      await User.updateProfile(form.name, form.idcard, form.phone, form.address);
      toast(t.okSave);
      await reload();
    } catch (e) { toast(e.message); }
  };

  const pickAvatar = async (i) => {
    try {
      await User.updateAvatar(i);
      await reload();
      setPickingAvatar(false);
      toast(t.okSave);
    } catch (e) { toast(e.message); }
  };

  const done = user?.profileDone;

  return (
    <div className="page">
      <div className="page-header"><div className="page-title">{t.me}</div></div>

      {/* 用户卡片 */}
      <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 16 }}>
        <div onClick={() => !isAdmin && setPickingAvatar(true)} style={{ cursor: isAdmin ? 'default' : 'pointer', position: 'relative' }}>
          <Avatar name={user?.name} idx={user?.avatar} size={56} />
          {!isAdmin && <div style={{ position: 'absolute', right: -2, bottom: -2, background: 'var(--orange)', color: '#fff', borderRadius: 10, fontSize: 10, padding: '2px 6px' }}>换</div>}
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ fontWeight: 800, fontSize: 17 }}>{user?.name || t.adminName}</div>
          <div style={{ fontSize: 13, color: 'var(--text-mute)', marginTop: 3 }}>
            ID {user?.id} · {isAdmin ? t.adminName : levelName(user?.level)}
          </div>
          <span className="badge" style={{ background: done ? '#EAF7EF' : '#FFF2F0', color: done ? '#127A3E' : '#C7361B', marginTop: 6, display: 'inline-block' }}>
            {done ? t.complete : t.incomplete}
          </span>
        </div>
      </div>

      {/* 资料编辑 */}
      {!isAdmin && (
        <div className="card" style={{ marginBottom: 16 }}>
          <div style={{ fontWeight: 700, marginBottom: 12 }}>{t.myProfile}</div>
          <div className="field"><label>{t.realName}</label><input value={form.name} onChange={set('name')} /></div>
          <div className="field"><label>{t.idCard}</label><input value={form.idcard} onChange={set('idcard')} /></div>
          <div className="field"><label>{t.phone}</label><input value={form.phone} onChange={set('phone')} /></div>
          <div className="field"><label>{t.address}</label><input value={form.address} onChange={set('address')} /></div>
          <button className="btn btn-primary btn-block" onClick={save}>{t.save}</button>
        </div>
      )}

      {/* 语言与登出 */}
      <div className="card" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontWeight: 600 }}>{t.language}</span>
          <button className="btn btn-ghost" style={{ fontSize: 13, padding: '8px 14px' }} onClick={() => setLang(lang === 'zh' ? 'en' : 'zh')}>
            {lang === 'zh' ? 'English' : '中文'}
          </button>
        </div>
      </div>
      <button className="btn btn-ghost btn-block" onClick={logout} style={{ color: 'var(--red)' }}>{t.logout}</button>

      {/* 头像选择 */}
      {pickingAvatar && (
        <Sheet title={t.avatarTitle} desc={t.avatarDesc} onClose={() => setPickingAvatar(false)}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 14 }}>
            {PAL.map((p, i) => (
              <div key={i} onClick={() => pickAvatar(i)} style={{ aspectRatio: '1', borderRadius: '50%', background: p, cursor: 'pointer', border: user?.avatar === i ? '3px solid var(--orange)' : '3px solid transparent' }} />
            ))}
          </div>
        </Sheet>
      )}
    </div>
  );
}
