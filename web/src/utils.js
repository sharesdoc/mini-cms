// 纯工具函数（无副作用，便于单元测试）

/**
 * 模板替换：tpl('再拓展 {n} 位', {n:3}) → '再拓展 3 位'。
 * @param {string} str 模板字符串
 * @param {Record<string,string|number>} map 占位符键值
 */
export function tpl(str, map) {
  let r = str || '';
  if (map) Object.keys(map).forEach((k) => { r = r.split('{' + k + '}').join(String(map[k])); });
  return r;
}

/**
 * 按级别表取级别名；越界回退到最低级名。
 * @param {string[]} lv 级别表（如 t.lv = ['','一级','二级','三级']）
 * @param {number} level 级别数字
 */
export function pickLevelName(lv, level) {
  const arr = lv || [];
  return arr[level] || arr[arr.length - 1] || '';
}

/**
 * 拼接查询参数（跳过 undefined/null/''）。
 * level 传 0 或空视为「全部」不传。
 */
export function qstr(obj) {
  const p = new URLSearchParams();
  Object.keys(obj || {}).forEach((k) => {
    const v = obj[k];
    if (v === undefined || v === null || v === '') return;
    p.append(k, v);
  });
  const s = p.toString();
  return s ? '?' + s : '';
}
