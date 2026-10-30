import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useI18n } from '../i18n/index.jsx';
import { useAuth } from '../store/auth.jsx';
import { Sheet } from './ui.jsx';

// 完善资料提示：未完善则每次登录展示，可"暂不填写"（仅本次会话不显示）
export default function ProfileNag() {
  const { t } = useI18n();
  const { user, isAdmin } = useAuth();
  const nav = useNavigate();
  const [dismissed, setDismissed] = useState(false);
  const show = !isAdmin && user && user.profileDone === false && !dismissed;

  if (!show) return null;
  return (
    <Sheet title={t.nagTitle} desc={t.nagDesc} onClose={() => setDismissed(true)}
      footer={<div style={{ display: 'flex', gap: 10 }}>
        <button className="btn btn-ghost btn-block" onClick={() => setDismissed(true)}>{t.nagLater}</button>
        <button className="btn btn-primary btn-block" onClick={() => { setDismissed(true); nav('/me'); }}>{t.nagOk}</button>
      </div>} />
  );
}
