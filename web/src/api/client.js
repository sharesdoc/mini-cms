// API 客户端：统一 fetch，自动注入 token，解析统一响应体 {code,msg,data}
import { qstr } from '../utils.js';

const BASE = ''; // 同源（dev 经 vite 代理 /api → svc:8080）

function getToken() { return localStorage.getItem('cms_token') || ''; }
export function setToken(t) { if (t) localStorage.setItem('cms_token', t); else localStorage.removeItem('cms_token'); }
export function getRole() { return localStorage.getItem('cms_role') || ''; }
export function setRole(r) { if (r) localStorage.setItem('cms_role', r); else localStorage.removeItem('cms_role'); }
export function clearSession() { setToken(null); setRole(null); localStorage.removeItem('cms_user'); }

async function request(method, url, body) {
  const headers = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const opt = { method, headers };
  if (body !== undefined) opt.body = JSON.stringify(body);
  let resp;
  try {
    resp = await fetch(BASE + url, opt);
  } catch (e) {
    throw new ApiError('网络错误，请检查后端服务是否启动', -1);
  }
  if (resp.status === 401) {
    // token 失效/过期：清本地会话 + 通知 AuthProvider 清 React state，
    // Protected 路由检测 role 为空后自动跳转 /login（任意页面均生效）
    clearSession();
    window.dispatchEvent(new Event('cms:auth-expired'));
    throw new ApiError('未登录或登录已失效', 40101);
  }
  let json;
  try { json = await resp.json(); } catch (e) { throw new ApiError('响应解析失败', -1); }
  if (json.code !== 0) {
    throw new ApiError(json.msg || '请求失败', json.code);
  }
  return json.data;
}

export class ApiError extends Error {
  constructor(msg, code) { super(msg); this.code = code; }
}

// 业务错误 → 本地化文案。校验类错误后端返回 i18n key 作为 msg。
export function localizeError(e, t) {
  if (!t || !(e instanceof ApiError)) return e.message;
  if (e.code === 40905 || e.code === 40906 || e.code === 40907) {
    return t[e.message] || e.message;
  }
  const map = {
    40901: t.errExists, 40902: t.errNoUser, 40903: t.errPwd, 40904: t.errDisabled,
    40101: t.errNoUser, 40001: e.message, 50000: e.message,
  };
  return map[e.code] || e.message;
}

export const api = {
  get: (u) => request('GET', u),
  post: (u, b) => request('POST', u, b),
  put: (u, b) => request('PUT', u, b),
  del: (u) => request('DELETE', u),
};

// 业务接口封装
export const Auth = {
  register: (username, password, confirmPwd) => api.post('/api/auth/register', { username, password, confirmPwd }),
  login: (username, password) => api.post('/api/auth/login', { username, password }),
  logout: () => api.post('/api/auth/logout'),
};
export const User = {
  me: () => api.get('/api/user/me'),
  updateProfile: (name, idcard, phone, address) => api.put('/api/user/profile', { name, idcard, phone, address }),
  updateAvatar: (avatar) => api.put('/api/user/avatar', { avatar }),
  home: () => api.get('/api/user/home'),
  expand: (targetId) => api.post('/api/user/expand', { targetId }),
};
export const Customers = {
  list: (level, q) => api.get('/api/customers' + qstr({ level, q })),
  roots: () => api.get('/api/customers/tree/roots'),
  children: (id) => api.get(`/api/customers/${id}/children`),
  detail: (id) => api.get(`/api/customers/${id}`),
};
export const Admin = {
  stats: () => api.get('/api/admin/stats'),
  setStatus: (id, status) => api.put(`/api/admin/customers/${id}/status`, { status }),
  remove: (id) => api.del(`/api/admin/customers/${id}`),
  restore: (id) => api.post(`/api/admin/customers/${id}/restore`),
  setQuota: (id, quota) => api.put(`/api/admin/customers/${id}/quota`, { quota }),
  config: () => api.get('/api/admin/config'),
  updateConfig: (quota, upgrade) => api.put('/api/admin/config', { quota, upgrade }),
};
export const Friends = {
  list: () => api.get('/api/friends'),
  search: (id) => api.get('/api/friends/search?id=' + encodeURIComponent(id)),
  add: (targetId) => api.post('/api/friends', { targetId }),
};
export const Groups = {
  list: () => api.get('/api/groups'),
  create: (name, memberIds) => api.post('/api/groups', { name, memberIds }),
  detail: (id) => api.get(`/api/groups/${id}`),
  addMembers: (id, userIds) => api.post(`/api/groups/${id}/members`, { userIds }),
  removeMember: (id, userId) => api.del(`/api/groups/${id}/members/${userId}`),
  setMuted: (id, muted) => api.put(`/api/groups/${id}/muted`, { muted }),
};
export const Messages = {
  thread: (scopeType, peerId, groupId) => api.get('/api/messages' + qstr({ scopeType, peerId, groupId })),
  send: (scopeType, peerId, groupId, content) => api.post('/api/messages', { scopeType, peerId, groupId, content }),
};


