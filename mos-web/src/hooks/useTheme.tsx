import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import type { ReactNode } from 'react';

/**
 * 主题色定义。
 *
 * 每套主题给出三个值：
 * - `primary`      主色（antd 的 colorPrimary）
 * - `rgb`          主色的 rgb 分量，给 `rgba(var(--mos-primary-rgb), α)` 用
 * - `primaryLight` 渐变的第二个色
 */
export interface ThemeDef {
  key: string;
  name: string;
  primary: string;
  rgb: string;
  primaryLight: string;
}

/** 可选主题色（按观感排序，默认是第一个） */
export const THEMES: ThemeDef[] = [
  { key: 'blue', name: '海蓝', primary: '#1677ff', rgb: '22, 119, 255', primaryLight: '#4096ff' },
  { key: 'teal', name: '青碧', primary: '#0d9488', rgb: '13, 148, 136', primaryLight: '#2dd4bf' },
  { key: 'indigo', name: '深靛', primary: '#4f46e5', rgb: '79, 70, 229', primaryLight: '#818cf8' },
  { key: 'cyan', name: '青蓝', primary: '#0891b2', rgb: '8, 145, 178', primaryLight: '#22d3ee' },
  { key: 'green', name: '松绿', primary: '#16a34a', rgb: '22, 163, 74', primaryLight: '#4ade80' },
  { key: 'orange', name: '暖橙', primary: '#ea580c', rgb: '234, 88, 12', primaryLight: '#fb923c' },
  { key: 'rose', name: '玫瑰', primary: '#e11d48', rgb: '225, 29, 72', primaryLight: '#fb7185' },
  { key: 'graphite', name: '石墨', primary: '#475569', rgb: '71, 85, 105', primaryLight: '#94a3b8' },
];

/** localStorage 键 */
const STORAGE_KEY = 'mos-theme';

/** 默认主题 */
export const DEFAULT_THEME = THEMES[0];

/** 读取已保存的主题（读不到就用默认） */
export function readStoredTheme(): string {
  try {
    const v = localStorage.getItem(STORAGE_KEY);
    return v && THEMES.some((t) => t.key === v) ? v : DEFAULT_THEME.key;
  } catch {
    return DEFAULT_THEME.key;
  }
}

/** 主题的 SVG favicon（跟随主色） */
function faviconSvg(theme: ThemeDef): string {
  return (
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">` +
    `<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">` +
    `<stop offset="0" stop-color="${theme.primary}"/>` +
    `<stop offset="1" stop-color="${theme.primaryLight}"/>` +
    `</linearGradient></defs>` +
    `<rect width="64" height="64" rx="15" fill="url(#g)"/>` +
    `<path d="M17.3 16.6 V47.4 M17.3 16.6 L32 33.3 L46.7 16.6 V47.4" ` +
    `fill="none" stroke="#fff" stroke-width="5.4" ` +
    `stroke-linecap="round" stroke-linejoin="round"/></svg>`
  );
}

/** 让浏览器标签页的图标 + 移动端地址栏颜色跟着主题色走 */
function applyFavicon(theme: ThemeDef) {
  if (typeof document === 'undefined') return;
  const url = `data:image/svg+xml,${encodeURIComponent(faviconSvg(theme))}`;
  let link = document.querySelector<HTMLLinkElement>("link[rel~='icon']");
  if (!link) {
    link = document.createElement('link');
    link.rel = 'icon';
    document.head.appendChild(link);
  }
  link.type = 'image/svg+xml';
  link.href = url;

  // 移动端浏览器地址栏 / 任务栏的主题色
  let meta = document.querySelector<HTMLMetaElement>(
    'meta[name="theme-color"]',
  );
  if (!meta) {
    meta = document.createElement('meta');
    meta.name = 'theme-color';
    document.head.appendChild(meta);
  }
  meta.content = theme.primary;
}

interface ThemeContextValue {
  /** 当前主题 */
  theme: ThemeDef;
  /** 当前主题的 key */
  themeKey: string;
  /** 切换主题（会写进 localStorage） */
  setThemeKey: (key: string) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

/**
 * 主题色 Provider。
 *
 * 做两件事：
 * 1. 把主题色写进 `<html>` 的 CSS 变量（`--mos-primary` 等），
 *    这样 `global.css` 里用 `var(--mos-primary)` 的地方自动跟随；
 * 2. 把主题给到 React 树里，让 antd 的 `ConfigProvider` 和切换按钮能用。
 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [themeKey, setThemeKeyState] = useState<string>(readStoredTheme);

  const theme = useMemo(
    () => THEMES.find((t) => t.key === themeKey) || DEFAULT_THEME,
    [themeKey],
  );

  const setThemeKey = useCallback((key: string) => {
    setThemeKeyState(key);
    try {
      localStorage.setItem(STORAGE_KEY, key);
    } catch {
      // 存不下（隐私模式）也无所谓，本次会话内仍然生效
    }
  }, []);

  // CSS 变量 + favicon 跟随主题
  useEffect(() => {
    const root = document.documentElement;
    root.style.setProperty('--mos-primary', theme.primary);
    root.style.setProperty('--mos-primary-rgb', theme.rgb);
    root.style.setProperty('--mos-primary-light', theme.primaryLight);
    root.dataset.mosTheme = theme.key;
    applyFavicon(theme);
  }, [theme]);

  const value = useMemo(
    () => ({ theme, themeKey, setThemeKey }),
    [theme, themeKey, setThemeKey],
  );

  return (
    <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
  );
}

/** 读取当前主题（必须在 `ThemeProvider` 内使用） */
export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme 必须在 ThemeProvider 内使用');
  return ctx;
}
