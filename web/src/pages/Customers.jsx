import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useI18n } from '../i18n/index.jsx';
import { useAuth } from '../store/auth.jsx';
import { Customers as CustApi } from '../api/client.js';
import { localizeError } from '../api/client.js';
import { Avatar, useToast } from '../components/ui.jsx';

export default function Customers() {
  const { t, levelName } = useI18n();
  const { isAdmin } = useAuth();
  const nav = useNavigate();
  const toast = useToast();

  const [view, setView] = useState(isAdmin ? 'tree' : 'list'); // list | tree
  const [level, setLevel] = useState(0);
  const [q, setQ] = useState('');
  const [list, setList] = useState([]);
  const [crumbs, setCrumbs] = useState([]); // 面包屑 [{id,name}]
  const [loading, setLoading] = useState(true);

  const badgeBg = (l) => l === 1 ? '#FFF0E4' : l === 2 ? '#EAF1FF' : '#F1F3F6';
  const badgeFg = (l) => l === 1 ? '#D9600B' : l === 2 ? '#2B5FD9' : '#5A616E';

  const loadList = async () => {
    setLoading(true);
    try { setList(await CustApi.list(level, q)); } catch (e) { toast(localizeError(e, t)); }
    finally { setLoading(false); }
  };
  const loadTree = async (parentId) => {
    setLoading(true);
    try {
      const rows = parentId ? await CustApi.children(parentId) : await CustApi.roots();
      setList(rows);
    } catch (e) { toast(localizeError(e, t)); }
    finally { setLoading(false); }
  };

  useEffect(() => {
    if (isAdmin && view === 'tree') loadTree(null);
    else if (isAdmin) loadList();
    else loadList();
  }, [view, level]);

  const search = () => { if (isAdmin && view === 'list') loadList(); };

  // 点击任意客户行 → 进入该客户详情页（资料 + 其下游列表）。
  // 深度浏览与后退在详情页内完成（下游项可继续点入、顶部返回后退），满足 REQ-NET-02。
  const drill = (row) => nav(`/customers/${row.id}`);
  const goCrumb = (idx) => {
    const next = crumbs.slice(0, idx);
    setCrumbs(next);
    loadTree(idx === 0 ? null : next[next.length - 1].id);
  };

  const title = isAdmin ? (view === 'tree' ? (crumbs.length ? crumbs[crumbs.length - 1].name : t.root) : (level ? levelName(level) : t.allCustomers)) : t.myCustomers;
  const levels = [0, 1, 2, 3].filter((l) => l === 0 || l < (t.lv.length));

  return (
    <div className="page">
      <div className="page-header"><div className="page-title">{t.net}</div></div>

      {/* 视图切换（管理员） */}
      {isAdmin && (
        <div style={{ display: 'flex', gap: 4, background: '#F3F4F7', borderRadius: 12, padding: 4, marginBottom: 12 }}>
          {['list', 'tree'].map((v) => {
            const on = view === v;
            return <button key={v} onClick={() => { setView(v); setCrumbs([]); }} style={{ flex: 1, border: 'none', borderRadius: 9, padding: '8px 0', fontWeight: 700, cursor: 'pointer', background: on ? '#fff' : 'transparent', color: on ? 'var(--text)' : 'var(--text-mute)' }}>{v === 'list' ? t.listView : t.treeView}</button>;
          })}
        </div>
      )}

      {/* 列表视图：筛选+搜索 */}
      {isAdmin && view === 'list' && (
        <div style={{ marginBottom: 12 }}>
          <div style={{ display: 'flex', gap: 8, marginBottom: 8, flexWrap: 'wrap' }}>
            {levels.map((l) => {
              const on = level === l;
              return <button key={l} onClick={() => setLevel(l)} className="badge" style={{ cursor: 'pointer', background: on ? '#FFF3E9' : '#fff', color: on ? 'var(--orange)' : 'var(--text-sub)', border: `1px solid ${on ? '#FF8A2B' : '#E4E7EC'}` }}>{l ? levelName(l) : t.allLevels}</button>;
            })}
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <input className="field" style={{ flex: 1, marginBottom: 0 }} value={q} onChange={(e) => setQ(e.target.value)} placeholder={t.searchPh} onKeyDown={(e) => e.key === 'Enter' && search()} />
            <button className="btn btn-primary" onClick={search} style={{ padding: '0 16px' }}>🔍</button>
          </div>
        </div>
      )}

      {/* 树形视图：面包屑 */}
      {isAdmin && view === 'tree' && crumbs.length > 0 && (
        <div style={{ display: 'flex', gap: 6, marginBottom: 12, flexWrap: 'wrap', alignItems: 'center' }}>
          <button onClick={() => goCrumb(0)} style={{ border: 'none', background: '#FFF3E9', color: 'var(--orange)', fontWeight: 700, padding: '4px 10px', borderRadius: 8, cursor: 'pointer' }}>{t.root}</button>
          {crumbs.map((c, i) => (
            <span key={c.id} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span style={{ color: 'var(--text-mute)' }}>›</span>
              <button onClick={() => goCrumb(i + 1)} style={{ border: 'none', background: i === crumbs.length - 1 ? '#FFF3E9' : '#F3F4F7', color: i === crumbs.length - 1 ? 'var(--orange)' : 'var(--text-sub)', fontWeight: 600, padding: '4px 10px', borderRadius: 8, cursor: 'pointer' }}>{c.name}</button>
            </span>
          ))}
        </div>
      )}

      <div style={{ fontWeight: 700, fontSize: 14, color: 'var(--text-sub)', margin: '4px 2px 10px' }}>{title} · {list.length}</div>

      {loading ? <div className="card">加载中…</div> :
        list.length ? list.map((row) => (
          <div className="row" key={row.id} onClick={() => drill(row)}>
            <Avatar name={row.name} idx={row.avatar} size={42} />
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontWeight: 600 }}>{row.name}</span>
                <span className="badge" style={{ background: badgeBg(row.level), color: badgeFg(row.level), fontSize: 10 }}>{levelName(row.level)}</span>
              </div>
              <div style={{ fontSize: 12, color: 'var(--text-mute)' }}>ID {row.id}</div>
            </div>
            {row.kidCount > 0 ? <span style={{ fontSize: 13, color: 'var(--orange)', fontWeight: 600 }}>{row.kidCount} ›</span> : <span style={{ fontSize: 12, color: 'var(--text-mute)' }}>{t.emptyLeaf}</span>}
          </div>
        )) : <div className="card" style={{ textAlign: 'center', color: 'var(--text-mute)', fontSize: 13 }}>{t.emptyList}</div>}
    </div>
  );
}
