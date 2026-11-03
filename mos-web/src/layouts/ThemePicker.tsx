import { CheckOutlined } from '@ant-design/icons';
import { THEMES, useTheme } from '@/hooks/useTheme';

/**
 * 主题色选择器。
 *
 * 展示所有可选主题色的小圆片，当前选中的打勾；
 * 点一下立刻生效（antd 的 ConfigProvider + CSS 变量都会跟着变）。
 */
export default function ThemePicker() {
  const { theme, themeKey, setThemeKey } = useTheme();

  return (
    <div className="mos-theme-picker">
      <div className="mos-theme-picker-title">主题色</div>
      <div className="mos-theme-swatches">
        {THEMES.map((t) => (
          <button
            key={t.key}
            type="button"
            className={`mos-theme-swatch${
              t.key === themeKey ? ' is-active' : ''
            }`}
            style={{
              background: `linear-gradient(135deg, ${t.primary}, ${t.primaryLight})`,
            }}
            onClick={() => setThemeKey(t.key)}
            title={t.name}
            aria-label={t.name}
            aria-pressed={t.key === themeKey}
          >
            {t.key === themeKey && <CheckOutlined />}
          </button>
        ))}
      </div>
      <div className="mos-theme-picker-current">
        当前：{theme.name}
      </div>
    </div>
  );
}
