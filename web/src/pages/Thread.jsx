import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useI18n } from '../i18n/index.jsx';
import { Messages as MsgApi, Groups } from '../api/client.js';
import { localizeError } from '../api/client.js';
import { Avatar, useToast } from '../components/ui.jsx';

export default function Thread() {
  const { kind, id } = useParams();
  const { t } = useI18n();
  const nav = useNavigate();
  const toast = useToast();
  const [msgs, setMsgs] = useState([]);
  const [text, setText] = useState('');
  const [title, setTitle] = useState('');
  const [grp, setGrp] = useState(null); // 群信息（含 isOwner/muted）
  const endRef = useRef(null);

  const isGroup = kind === 'group';

  const load = async () => {
    try {
      const list = await MsgApi.thread(isGroup ? 'group' : 'dm', isGroup ? null : id, isGroup ? id : null);
      setMsgs(list);
      if (isGroup) {
        const g = await Groups.detail(id);
        setGrp(g); setTitle(g.name);
      } else {
        setTitle(list.length ? '' : ''); // 单聊标题用对方名，懒加载
      }
    } catch (e) { toast(localizeError(e, t)); }
  };
  useEffect(() => { load(); }, [kind, id]);
  useEffect(() => { endRef.current && endRef.current.scrollIntoView({ behavior: 'smooth' }); }, [msgs]);

  const send = async () => {
    const c = text.trim();
    if (!c) return;
    try {
      await MsgApi.send(isGroup ? 'group' : 'dm', isGroup ? null : id, isGroup ? id : null, c);
      setText('');
      load();
    } catch (e) { toast(localizeError(e, t)); }
  };

  const toggleMute = async () => {
    if (!grp?.isOwner) return;
    try { await Groups.setMuted(id, grp.muted === 1 ? 0 : 1); load(); } catch (e) { toast(localizeError(e, t)); }
  };

  const muted = isGroup && grp && grp.muted === 1 && !grp?.isOwner;

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', background: 'var(--bg)' }}>
      {/* 顶栏 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '12px 14px', background: '#fff', borderBottom: '1px solid var(--border)', position: 'sticky', top: 0, zIndex: 10 }}>
        <button onClick={() => nav('/messages')} className="btn btn-ghost" style={{ padding: '6px 10px' }}>‹</button>
        <div style={{ flex: 1, fontWeight: 800 }}>
          {isGroup ? (title || t.groups) : (msgs.find((m) => !m.mine)?.senderName || t.friends)}
        </div>
        {isGroup && grp?.isOwner && (
          <button onClick={toggleMute} className="badge" style={{ border: 'none', cursor: 'pointer', background: grp.muted === 1 ? '#FFF2F0' : '#EAF7EF', color: grp.muted === 1 ? 'var(--red)' : 'var(--green)' }}>
            {grp.muted === 1 ? t.muteOn : t.muteOff}
          </button>
        )}
      </div>

      {/* 消息列表 */}
      <div style={{ flex: 1, padding: '14px', overflowY: 'auto', maxWidth: 880, width: '100%', margin: '0 auto' }}>
        {msgs.length ? msgs.map((m) => {
          const mine = m.mine;
          return (
            <div key={m.id} style={{ display: 'flex', justifyContent: mine ? 'flex-end' : 'flex-start', marginBottom: 12 }}>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: mine ? 'flex-end' : 'flex-start', maxWidth: '76%' }}>
                {!mine && isGroup && <div style={{ fontSize: 11, color: 'var(--text-mute)', marginBottom: 3 }}>{m.senderName}</div>}
                <div style={{ padding: '9px 13px', borderRadius: mine ? '16px 16px 5px 16px' : '16px 16px 16px 5px', background: mine ? 'linear-gradient(180deg,#FF9138,#FF6A00)' : '#fff', color: mine ? '#fff' : 'var(--text)', border: mine ? 'none' : '1px solid #EDEFF3', fontSize: 14, lineHeight: 1.5, wordBreak: 'break-word' }}>
                  {m.content}
                </div>
                <div style={{ fontSize: 10, color: 'var(--text-mute)', marginTop: 3 }}>{m.createdAt ? m.createdAt.slice(11, 16) : ''}</div>
              </div>
            </div>
          );
        }) : <div style={{ textAlign: 'center', color: 'var(--text-mute)', marginTop: 40, fontSize: 13 }}>{t.sayHi}</div>}
        <div ref={endRef} />
      </div>

      {/* 输入栏 */}
      <div style={{ padding: '10px 14px calc(10px + env(safe-area-inset-bottom))', background: '#fff', borderTop: '1px solid var(--border)', display: 'flex', gap: 8, maxWidth: 880, width: '100%', margin: '0 auto' }}>
        <input value={text} onChange={(e) => setText(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && send()}
          disabled={muted} placeholder={muted ? t.mutedPh : t.msgPh}
          style={{ flex: 1, border: '1px solid #E2E5EB', borderRadius: 13, padding: '11px 14px', fontSize: 15, background: muted ? '#F3F4F7' : '#FAFBFC' }} />
        <button className="btn btn-primary" onClick={send} disabled={muted || !text.trim()} style={{ padding: '0 18px' }}>↑</button>
      </div>
    </div>
  );
}
