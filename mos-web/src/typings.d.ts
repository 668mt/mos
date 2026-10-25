/** dplayer 缺少官方类型声明，此处补充最小可用类型 */
declare module 'dplayer' {
  export interface DPlayerOptions {
    container: HTMLElement | string;
    url?: string;
    autoplay?: boolean;
    loop?: boolean;
    hotkey?: boolean;
    danmaku?: boolean;
    pictureInPicture?: boolean;
    quality?: { default: number; html5: number }[];
    [key: string]: any;
  }

  export default class DPlayer {
    constructor(options: DPlayerOptions);
    destroy(): void;
    play(): void;
    pause(): void;
    [key: string]: any;
  }
}
