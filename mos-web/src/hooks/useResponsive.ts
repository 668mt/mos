import { useEffect, useState } from 'react';

/** 断点：与 antd 的 Grid 一致 */
export const BREAKPOINTS = {
  xs: 480,
  sm: 768,
  md: 992,
  lg: 1200,
  xl: 1600,
} as const;

export type Breakpoint = keyof typeof BREAKPOINTS;

/** 响应式 Hook：返回当前断点与是否为移动端 */
export function useResponsive(): {
  breakpoint: Breakpoint;
  isMobile: boolean;
  isTablet: boolean;
  width: number;
} {
  const getBreakpoint = (): Breakpoint => {
    if (typeof window === 'undefined') return 'lg';
    const w = window.innerWidth;
    if (w < BREAKPOINTS.xs) return 'xs';
    if (w < BREAKPOINTS.sm) return 'sm';
    if (w < BREAKPOINTS.md) return 'md';
    if (w < BREAKPOINTS.lg) return 'lg';
    return 'xl';
  };

  const [width, setWidth] = useState<number>(
    typeof window === 'undefined' ? 1200 : window.innerWidth,
  );
  const [breakpoint, setBreakpoint] = useState<Breakpoint>(getBreakpoint);

  useEffect(() => {
    let raf = 0;
    const onResize = () => {
      cancelAnimationFrame(raf);
      raf = requestAnimationFrame(() => {
        setWidth(window.innerWidth);
        setBreakpoint(getBreakpoint());
      });
    };
    window.addEventListener('resize', onResize);
    window.addEventListener('orientationchange', onResize);
    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('resize', onResize);
      window.removeEventListener('orientationchange', onResize);
    };
  }, []);

  return {
    breakpoint,
    isMobile: breakpoint === 'xs' || breakpoint === 'sm',
    isTablet: breakpoint === 'md',
    width,
  };
}
