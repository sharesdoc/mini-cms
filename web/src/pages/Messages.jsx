import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useI18n } from '../i18n/index.jsx';
import { Friends, Groups } from '../api/client.js';
import { localizeError } from '../api/client.js';
import { Avatar, PAL, useToast, Sheet } from '../components/ui.jsx';

export default function Messages() {
  const { t, tpl } = useI18n();
  const nav = useNavigate();
  const toast = useToast();
  const [tab, setTab] = useState('friends');
  const [friends, setFriends] = useState([]);
  const [groups, setGroups] = useState([]);

  // 加好友弹层
  const [addOpen, setAddOpen] = useState(false);
  const [searchId, setSearchId] = useState('');
  const [candidate, setCandidate] = useState(null);
  const [searching, setSearching] = useState(false);

  // 建群弹层
  const [grpOpen, setGrpOpen] = useState(false);
  const [grpName, setGrpName] = useState('');
  const [picks, setPicks] = useState([]);

  const loadFriends = async () => { try { setFriends(await Friends.list()); } catch (e) { toast(localizeError(e, t)); } };
  const loadGroups = async () => { try { setGroups(await Groups.list()); } catch (e) { toast(localizeError(e, t)); } };
  useEffect(() => { loadFriends(); loadGroups(); }, []);

  const doSearch = async () => {
    setSearching(true); setCandidate(null);
    try { setCandidate(await Friends.search(searchId.trim())); } catch (e) { toast(localizeError(e, t)); }
    finally { setSearching(false); }
  };
  const doAdd = async () => {
    if (!candidate) return;
    try { await Friends.add(candidate.id); toast(tpl(t.okFriend, { n: candidate.name })); setAddOpen(false); setSearchId(''); setCandidate(null); loadFriends(); }
    catch (e) { toast(localizeError(e, t)); }
  };

  const togglePick = (fid) => setPicks(picks.includes(fid) ? picks.filter((x) => x !== fid) : [...picks, fid]);
  const doCreateGroup = async () => {
    if (!grpName.trim()) { toast(t.errGroupName); return; }
    if (!picks.length) { toast(t.errGroupMember); return; }
    try {
      await Groups.create(grpName.trim(), picks);
      toast(tpl(t.okGroup, { n: grpName.trim() }));
      setGrpOpen(false); setGrpName(''); setPicks([]); setTab('groups'); loadGroups();
    } catch (e) { toast(localizeError(e, t)); }
  };

  const ftime = (ts) => ts ? ts.slice(5, 10) : '';

  return (
    <div className="page">
      <div className="page-header"><div className="page-title">{t.chat}</div></div>

      <div style={{ display: 'flex', gap: 4, background: '#F3F4F7', borderRadius: 12, padding: 4, marginBottom: 12 }}>
        {['friends', 'groups'].map((tb) => {
          const on = tab === tb;
          return <button key={tb} onClick={() => setTab(tb)} style={{ flex: 1, border: 'none', borderRadius: 9, padding: '8px 0', fontWeight: 700, cursor: 'pointer', background: on ? '#fff' : 'transparent', color: on ? 'var(--text)' : 'var(--text-mute)' }}>{tb === 'friends' ? t.friends : t.groups}</button>;
        })}
      </div>

      {tab === 'friends' ? (
        <>
          <button className="btn btn-primary btn-block" style={{ marginBottom: 12 }} onClick={() => setAddOpen(true)}>{t.addFriend}</button>
          {friends.length ? friends.map((f) => (
            <div className="row" key={f.id} onClick={() => nav(`/messages/dm/${f.id}`)}>
              <Avatar name={f.name} idx={f.avatar} size={44} />
              <div style={{ flex: 1 }}><div style={{ fontWeight: 600 }}>{f.name}</div><div style={{ fontSize: 12, color: 'var(--text-mute)' }}>ID {f.id}</div></div>
            </div>
          )) : <div className="card" style={{ textAlign: 'center', color: 'var(--text-mute)', fontSize: 13 }}>{t.emptyFriends}</div>}
        </>
      ) : (
        <>
          <button className="btn btn-primary btn-block" style={{ marginBottom: 12 }} onClick={() => friends.length ? setGrpOpen(true) : toast(t.groupNoFriend)}>{t.createGroup}</button>
          {groups.length ? groups.map((g) => (
            <div className="row" key={g.id} onClick={() => nav(`/messages/group/${g.id}`)}>
              <div className="avatar" style={{ width: 44, height: 44, background: PAL[(g.name.length + 2) % 8], fontSize: 18 }}>{(g.name || '?').charAt(0)}</div>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600 }}>{g.name}</div>
                <div style={{ fontSize: 12, color: 'var(--text-mute)' }}>{tpl(t.members, { n: g.memberCount })}{g.lastText ? ' · ' + g.lastText : ''}</div>
              </div>
              {g.muted === 1 && <span style={{ fontSize: 11, color: 'var(--red)' }}>{t.muteOn}</span>}
            </div>
          )) : <div className="card" style={{ textAlign: 'center', color: 'var(--text-mute)', fontSize: 13 }}>{t.emptyGroups}</div>}
        </>
      )}

      {/* 加好友 */}
      {addOpen && (
        <Sheet title={t.friendTitle} desc={t.friendDesc} onClose={() => { setAddOpen(false); setCandidate(null); setSearchId(''); }}
          footer={candidate ? <button className="btn btn-primary btn-block" onClick={doAdd}>{t.friendOk}</button> : null}>
          <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
            <input className="field" style={{ flex: 1, marginBottom: 0 }} value={searchId} onChange={(e) => setSearchId(e.target.value)} placeholder={t.friendPh} onKeyDown={(e) => e.key === 'Enter' && doSearch()} />
            <button className="btn btn-primary" onClick={doSearch} style={{ padding: '0 16px' }}>🔍</button>
          </div>
          {searching && <div style={{ fontSize: 13, color: 'var(--text-mute)' }}>…</div>}
          {candidate ? (
            <div className="row" style={{ marginTop: 8 }}>
              <Avatar name={candidate.name} idx={candidate.avatar} size={40} />
              <div><div style={{ fontWeight: 600 }}>{candidate.name}</div><div style={{ fontSize: 12, color: 'var(--text-mute)' }}>ID {candidate.id}</div></div>
            </div>
          ) : (searchId && !searching ? <div style={{ fontSize: 13, color: 'var(--text-mute)' }}>{t.noCandidates}</div> : <div style={{ fontSize: 13, color: 'var(--text-mute)' }}>{t.friendIdleTip}</div>)}
        </Sheet>
      )}

      {/* 建群 */}
      {grpOpen && (
        <Sheet title={t.groupTitle} desc={t.groupDesc} onClose={() => { setGrpOpen(false); setPicks([]); setGrpName(''); }}
          footer={<button className="btn btn-primary btn-block" onClick={doCreateGroup}>{t.groupOk}</button>}>
          <div className="field"><input value={grpName} onChange={(e) => setGrpName(e.target.value)} placeholder={t.groupPh} /></div>
          <div style={{ fontSize: 13, color: 'var(--text-sub)', marginBottom: 8 }}>{tpl(t.pickedN, { n: picks.length })}</div>
          {friends.map((f) => {
            const on = picks.includes(f.id);
            return (
              <div key={f.id} onClick={() => togglePick(f.id)} className="row" style={{ background: on ? '#FFF8F2' : '#fff', borderColor: on ? '#FF8A2B' : 'var(--border)' }}>
                <Avatar name={f.name} idx={f.avatar} size={38} />
                <div style={{ flex: 1, fontWeight: 600 }}>{f.name}</div>
                <div style={{ width: 22, height: 22, borderRadius: 6, background: on ? 'var(--orange)' : '#fff', border: `2px solid ${on ? 'var(--orange)' : '#D7DBE2'}`, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 14 }}>{on ? '✓' : ''}</div>
              </div>
            );
          })}
        </Sheet>
      )}
    </div>
  );
}
