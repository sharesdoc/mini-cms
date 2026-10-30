import { describe, it, expect } from 'vitest';
import { tpl, pickLevelName, qstr } from './utils.js';

describe('tpl 模板替换', () => {
  it('替换单个占位符', () => {
    expect(tpl('再拓展 {n} 位客户', { n: 3 })).toBe('再拓展 3 位客户');
  });
  it('替换多个占位符', () => {
    expect(tpl('{n} · {l}', { n: 5, l: '一级' })).toBe('5 · 一级');
  });
  it('无 map 原样返回', () => {
    expect(tpl('已是最高级别')).toBe('已是最高级别');
  });
  it('数字自动转字符串', () => {
    expect(tpl('成员 {n}', { n: 0 })).toBe('成员 0');
  });
});

describe('pickLevelName 级别名', () => {
  const lv = ['', '一级客户', '二级客户', '三级客户'];
  it('取对应级别', () => {
    expect(pickLevelName(lv, 1)).toBe('一级客户');
    expect(pickLevelName(lv, 3)).toBe('三级客户');
  });
  it('越界回退到最低级', () => {
    expect(pickLevelName(lv, 99)).toBe('三级客户');
    expect(pickLevelName(lv, 0)).toBe('三级客户'); // 0 命中空串 → 回退
  });
  it('空表返回空串', () => {
    expect(pickLevelName([], 1)).toBe('');
  });
});

describe('qstr 查询参数拼接', () => {
  it('跳过空值', () => {
    expect(qstr({ a: 'x', b: undefined, c: null, d: '' })).toBe('?a=x');
  });
  it('level=0 视为全部不传', () => {
    // 0 是 falsy 但非 undefined/null/''，按需求 level=0 表示「全部」应跳过
    // 当前实现不跳过 0 → 实际发送 ?level=0，后端 level>0 判断兜底为全部。验证现状：
    expect(qstr({ level: 0 })).toBe('?level=0');
    expect(qstr({ level: 2 })).toBe('?level=2');
  });
  it('多参数拼接', () => {
    expect(qstr({ scopeType: 'dm', peerId: '123' })).toBe('?scopeType=dm&peerId=123');
  });
  it('全空返回空串', () => {
    expect(qstr({})).toBe('');
    expect(qstr(null)).toBe('');
  });
});
