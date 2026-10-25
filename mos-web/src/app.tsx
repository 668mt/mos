import { ConfigProvider, App as AntdApp, theme as antdTheme } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { readCurrentUser } from '@/models/account';
// 给 antd 静态确认框（Modal.confirm 等）的 onOk 加上全局兜底：
// 避免 onOk 的 rejection 被 antd 二次抛出后变成 Unhandled Rejection，
// 进而被 umi 的错误浮层盖住整页。详见 utils/antdSafe.ts 的注释。
import '@/utils/antdSafe';
import './global.css';

const token = {
  colorPrimary: '#1677ff',
  borderRadius: 8,
  fontSize: 14,
  colorBgLayout: '#f0f2f5',
  wireframe: false,
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
        background: #f0f2f5;
      }
      #mos-loading .ring {
        width: 36px; height: 36px; border: 3px solid rgba(22,119,255,.15);
        border-top-color: #1677ff; border-radius: 50%;
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
 * 全局错误边界：注入 antd 的 ConfigProvider 与中文语言包。
 */
export function rootContainer(container: any) {
  injectLoadingPlaceholder();
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{ token, algorithm: antdTheme.defaultAlgorithm }}
    >
      <AntdApp>{container}</AntdApp>
    </ConfigProvider>
  );
}
