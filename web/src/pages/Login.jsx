import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useI18n } from '../i18n/index.jsx';
import { useAuth } from '../store/auth.jsx';
import { Auth } from '../api/client.js';
import { ApiError } from '../api/client.js';
import { useToast } from '../components/ui.jsx';

export default function Login() {
  const { t, setLang, lang } = useI18n();
  const { loginSession } = useAuth();
  const nav = useNavigate();
  const toast = useToast();
  const [mode, setMode] = useState('login'); // login | register
  const [form, setForm] = useState({ username: '', password: '', password2: '' });
  const [err, setErr] = useState('');
  const [loading, setLoading] = useState(false);

  // 已登录则跳转
  useEffect(() => {
    if (localStorage.getItem('cms_role')) {
      nav(localStorage.getItem('cms_role') === 'ADMIN' ? '/stats' : '/home', { replace: true });
    }
  }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const userPh = mode === 'register' ? t.regPh : t.loginPh;

  const submit = async () => {
    setErr('');
    const un = form.username.trim();
    if (!un || !form.password) { setErr(t.errEmpty); return; }
    setLoading(true);
    try {
      const data = mode === 'register'
        ? await Auth.register(un, form.password, form.password2)
        : await Auth.login(un, form.password);
      loginSession(data.token, data.role, data.user);
      toast(mode === 'register' ? t.registerBtn + ' ✓' : t.login + ' ✓');
      // 管理员→报表；客户未完善资料→停留在首页由 nag 提示
      nav(data.role === 'ADMIN' ? '/stats' : '/home', { replace: true });
    } catch (e) {
      setErr(e instanceof ApiError ? mapErr(e, t) : (e.message || '错误'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '24px 16px' }}>
      <div style={{ width: '100%', maxWidth: 400, animation: 'flyUp .5s cubic-bezier(.2,.8,.2,1) both' }}>
        {/* logo */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 14, marginBottom: 26 }}>
          <div style={{ width: 60, height: 60, borderRadius: 19, background: 'linear-gradient(150deg,#FFA24D,#FF6A00)', boxShadow: '0 12px 30px -10px rgba(255,106,0,.65)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontWeight: 800, fontSize: 21 }}>
            C<span style={{ opacity: .65 }}>M</span>S
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 22, fontWeight: 800 }}>{t.appName}</div>
            <div style={{ fontSize: 13, color: 'var(--text-mute)', marginTop: 5 }}>{t.tagline}</div>
          </div>
        </div>

        <div className="card" style={{ borderRadius: 22, padding: 22 }}>
          {/* tab */}
          <div style={{ display: 'flex', gap: 4, background: '#F3F4F7', borderRadius: 12, padding: 4, marginBottom: 20 }}>
            {['login', 'register'].map((m) => {
              const on = mode === m;
              return (
                <button key={m} onClick={() => { setMode(m); setErr(''); }}
                  style={{ flex: 1, border: 'none', borderRadius: 9, padding: '9px 0', fontSize: 14, fontWeight: 700, cursor: 'pointer', background: on ? '#fff' : 'transparent', color: on ? 'var(--text)' : 'var(--text-mute)', boxShadow: on ? '0 1px 3px rgba(16,20,28,.14)' : 'none' }}>
                  {m === 'login' ? t.login : t.register}
                </button>
              );
            })}
          </div>

          <div className="field">
            <label>{t.username}</label>
            <input value={form.username} onChange={set('username')} placeholder={userPh} />
          </div>
          <div className="field">
            <label>{t.password}</label>
            <input type="password" value={form.password} onChange={set('password')} placeholder={t.passwordPh} />
          </div>
          {mode === 'register' && (
            <div className="field">
              <label>{t.confirmPw}</label>
              <input type="password" value={form.password2} onChange={set('password2')} placeholder={t.confirmPwPh} />
            </div>
          )}

          {err && <div style={{ color: 'var(--red)', fontSize: 13, marginBottom: 12 }}>{err}</div>}

          <button className="btn btn-primary btn-block" onClick={submit} disabled={loading}>
            {loading ? '…' : (mode === 'login' ? t.loginBtn : t.registerBtn)}
          </button>

          {/* 演示账号提示 */}
          <div style={{ marginTop: 16, fontSize: 12, color: 'var(--text-mute)', textAlign: 'center', lineHeight: 1.6 }}>
            {t.demoTip}：<br />
            {t.demoAdminConfig}
          </div>
        </div>

        {/* 语言切换 */}
        <div style={{ textAlign: 'center', marginTop: 18 }}>
          <button onClick={() => setLang(lang === 'zh' ? 'en' : 'zh')} className="btn btn-ghost" style={{ fontSize: 13, padding: '8px 16px' }}>
            {t.language}
          </button>
        </div>
      </div>
    </div>
  );
}

// 后端错误码 → 本地化文案
function mapErr(e, t) {
  switch (e.code) {
    case 40901: return t.errExists;
    case 40902: return t.errNoUser;
    case 40903: return t.errPwd;
    case 40904: return t.errDisabled;
    case 40001: return e.message;
    default: return e.message || t.errEmpty;
  }
}
