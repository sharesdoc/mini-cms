import React, { createContext, useContext, useEffect, useState } from 'react';
import { T } from './dict.js';
import { tpl, pickLevelName } from '../utils.js';

// i18n Context：当前语言 + t 字典 + setLang（持久化 localStorage）
const I18nContext = createContext(null);

export function I18nProvider({ children }) {
  const [lang, setLangState] = useState(() => localStorage.getItem('cms_lang') || 'zh');
  const setLang = (l) => {
    setLangState(l);
    localStorage.setItem('cms_lang', l);
    document.documentElement.lang = l;
  };
  useEffect(() => { document.documentElement.lang = lang; }, [lang]);

  const t = T[lang] || T.zh;
  // 级别名（按系统级别数）
  const levelName = (l) => pickLevelName(t.lv, l);

  return (
    <I18nContext.Provider value={{ lang, setLang, t, tpl, levelName }}>
      {children}
    </I18nContext.Provider>
  );
}

export function useI18n() {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error('useI18n must be used within I18nProvider');
  return ctx;
}
