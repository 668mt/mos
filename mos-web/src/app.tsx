import { ConfigProvider, App as AntdApp, theme as antdTheme } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import type { ReactNode } from 'react';
import { readCurrentUser } from '@/models/account';
import { ThemeProvider, useTheme } from '@/hooks/useTheme';
// 给 antd 静态确认框（Modal.confirm 等）的 onOk 加上全局兜底：
// 避免 onOk 的 rejection 被 antd 二次抛出后变成 Unhandled Rejection，
// 进而被 umi 的错误浮层盖住整页。详见 utils/antdSafe.ts 的注释。
import '@/utils/antdSafe';
import './global.css';

/**
 * 全局设计 token。
 *
 * 和 `global.css` 里的 `--mos-*` 变量保持一致：
 * 颜色、圆角、阴影都集中在这里定，页面里不要各写各的。
 */
const token = {
  colorPrimary: '#1677ff',
  colorInfo: '#1677ff',
  colorSuccess: '#16a34a',
  colorWarning: '#f59e0b',
  colorError: '#ef4444',
  colorTextBase: '#1c2434',
  colorBgLayout: '#f4f6fb',
  colorBgContainer: '#ffffff',
  colorBorder: '#e3e8f0',
  colorBorderSecondary: '#eef1f6',
  borderRadius: 8,
  borderRadiusLG: 12,
  fontSize: 14,
  controlHeight: 34,
  wireframe: false,
};

/**
 * 组件级微调。
 *
 * 只调「质感」相关的（阴影、表头底色、字重），
 * 不改布局和交互 —— 布局统一在 `global.css` 里。
 */
const components = {
  Button: {
    // 主按钮带一点发光阴影，视觉上更精致
    primaryShadow: '0 4px 12px rgba(22, 119, 255, 0.26)',
    fontWeight: 500,
    defaultShadow: '0 1px 2px rgba(16, 24, 40, 0.04)',
  },
  Table: {
    headerBg: '#f7f9fc',
    headerColor: '#5a6478',
    headerSplitColor: 'transparent',
    rowHoverBg: '#f5f9ff',
    borderColor: '#eef1f6',
  },
  Card: {
    headerFontSize: 15,
  },
  Menu: {
    itemBorderRadius: 8,
    itemHeight: 40,
    itemMarginInline: 4,
  },
  Modal: {
    titleFontSize: 16,
    borderRadiusLG: 14,
  },
  Tag: {
    defaultBg: '#f2f5f9',
  },
  Input: {
    activeShadow: '0 0 0 3px rgba(22, 119, 255, 0.1)',
  },
  Select: {
    activeOutlineColor: 'rgba(22, 119, 255, 0.1)',
  },
};

/**
 * 初始化全局状态：读取本地保存的登录用户。
 */
export async function getInitialState(): Promise<any> {
  const currentUser = readCurrentUser();
  return {
    currentUser,
    settings: { navTheme: 'light', layout: 'side' },
  };
}

/**
 * 首屏加载占位。
 *
 * Umi 生成的 index.html 里没有加载态，这里在 React 挂载前动态插入，
 * 避免白屏与「正在加载」无反馈。
 */
function injectLoadingPlaceholder() {
  if (typeof document === 'undefined') return;
  const root = document.getElementById('root');
  if (!root || root.querySelector('#mos-loading')) return;

  const wrap = document.createElement('div');
  wrap.id = 'mos-loading';
  wrap.innerHTML = `
    <style>
      #mos-loading {
        position: fixed; inset: 0; display: flex; flex-direction: column;
        align-items: center; justify-content: center; gap: 16px;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
          'PingFang SC', 'Microsoft YaHei', sans-serif; color: rgba(0,0,0,.45);
        background: #f4f6fb;
      }
      #mos-loading .ring {
        width: 36px; height: 36px; border: 3px solid rgba(var(--mos-primary-rgb, 22, 119, 255), .15);
        border-top-color: var(--mos-primary, #1677ff); border-radius: 50%;
        animation: mos-spin .8s linear infinite;
      }
      @keyframes mos-spin { to { transform: rotate(360deg); } }
    </style>
    <div class="ring"></div>
    <div>正在加载 MOS 控制台...</div>
  `;
  root.appendChild(wrap);
}

/**
 * 按当前主题色渲染 antd。
 *
 * 单独抽一个组件，是因为 `theme` 依赖 `useTheme()` ——
 * 必须挂在 `ThemeProvider` 里面才能读到。
 */
function ThemedApp({ children }: { children: ReactNode }) {
  const { theme } = useTheme();
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          ...token,
          colorPrimary: theme.primary,
          colorInfo: theme.primary,
        },
        components: {
          ...components,
          // 这几处是「主色的透明变体」，必须跟着主题色走
          Button: {
            ...components.Button,
            primaryShadow: `0 4px 12px rgba(${theme.rgb}, 0.26)`,
          },
          Input: {
            ...components.Input,
            activeShadow: `0 0 0 3px rgba(${theme.rgb}, 0.1)`,
          },
          Select: {
            ...components.Select,
            activeOutlineColor: `rgba(${theme.rgb}, 0.1)`,
          },
          Table: {
            ...components.Table,
            rowHoverBg: `rgba(${theme.rgb}, 0.04)`,
          },
        },
        algorithm: antdTheme.defaultAlgorithm,
      }}
    >
      <AntdApp>{children}</AntdApp>
    </ConfigProvider>
  );
}

/**
 * 全局错误边界：注入 antd 的 ConfigProvider 与中文语言包。
 */
export function rootContainer(container: any) {
  injectLoadingPlaceholder();
  return (
    <ThemeProvider>
      <ThemedApp>{container}</ThemedApp>
    </ThemeProvider>
  );
}
