import { describe, it, expect } from 'vitest';
import { ApiError, localizeError } from './client.js';

// 模拟 i18n 字典子集（与 dict.js 一致的 key）
const t = {
  errExists: '该用户名已被注册',
  errNoUser: '用户名不存在',
  errPwd: '密码不正确',
  errDisabled: '账号已被禁用',
  errSelf: '不能拓展自己',
  errFriendExists: '你们已经是好友了',
  muted: '群主已开启全员禁言',
};

describe('localizeError 业务错误本地化', () => {
  it('用户名已存在 40901 → errExists', () => {
    expect(localizeError(new ApiError('errExists', 40901), t)).toBe('该用户名已被注册');
  });
  it('用户名不存在 40902 → errNoUser', () => {
    expect(localizeError(new ApiError('x', 40902), t)).toBe('用户名不存在');
  });
  it('密码错误 40903 → errPwd', () => {
    expect(localizeError(new ApiError('x', 40903), t)).toBe('密码不正确');
  });
  it('禁用 40904 → errDisabled', () => {
    expect(localizeError(new ApiError('x', 40904), t)).toBe('账号已被禁用');
  });
  it('拓展校验失败 40905 → 按 i18n key 查字典', () => {
    // 后端 msg 为 i18n key（如 errSelf）
    expect(localizeError(new ApiError('errSelf', 40905), t)).toBe('不能拓展自己');
  });
  it('好友校验失败 40906 → 按 i18n key 查字典', () => {
    expect(localizeError(new ApiError('errFriendExists', 40906), t)).toBe('你们已经是好友了');
  });
  it('群校验失败 40907 → 按 i18n key 查字典（muted）', () => {
    expect(localizeError(new ApiError('muted', 40907), t)).toBe('群主已开启全员禁言');
  });
  it('未知 key 回退到原始 message', () => {
    expect(localizeError(new ApiError('未定义的key', 40905), t)).toBe('未定义的key');
  });
  it('无 t 字典时回退到 message', () => {
    expect(localizeError(new ApiError('plain', 40905), null)).toBe('plain');
  });
  it('非 ApiError 直接返回 message', () => {
    expect(localizeError(new Error('网络错误'), t)).toBe('网络错误');
  });
});
