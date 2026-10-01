import type { ThemeConfig } from 'antd'

/**
 * 科技蓝色系完整色阶（blue-1 最浅 → blue-10 最深），视觉基调的唯一事实源。
 * 调整主色只改这里，经 ConfigProvider 全站即时生效。
 */
export const palette = {
  blue1: '#e6f4ff',
  blue2: '#bae0ff',
  blue3: '#91caff',
  blue4: '#69b1ff',
  blue5: '#40a9ff',
  blue6: '#1677ff',
  blue7: '#0958d9',
  blue8: '#003eb3',
  blue9: '#002c8c',
  blue10: '#001a57',
} as const

/** 中性灰阶（背景/边框/文字），对齐 AntD 中性色板命名 */
export const neutrals = {
  gray1: '#ffffff',
  gray2: '#fafafa',
  gray3: '#f5f5f5',
  gray4: '#f0f0f0',
  gray5: '#d9d9d9',
  gray6: '#bfbfbf',
  gray7: '#8c8c8c',
  gray8: '#595959',
  gray9: '#434343',
  gray10: '#1f1f1f',
} as const

/** success/warning/danger 沿用 AntD 语义色默认值，不覆写；暗色仅预留，见 index.tsx */
export const colorPrimary = palette.blue6

/** 间距规范（4 的倍数栅格）：页面内布局用，非 antd token 消费点 */
export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
} as const

/** 控件与排版规范 */
export const layout = {
  /** 全局圆角（seed token borderRadius 同源，组件内不得散写） */
  borderRadius: 8,
  /** 登录表单卡片宽度（Task 2.4 定案基线） */
  loginCardWidth: 380,
  /** 侧栏展开宽度 */
  siderWidth: 224,
} as const

/** AntD v6 Design Tokens 主题配置：seed 层集中定义，派生层级交由 algorithm */
export const antdTheme: ThemeConfig = {
  token: {
    colorPrimary,
    colorInfo: palette.blue6,
    colorSuccess: '#52c41a',
    colorWarning: '#faad14',
    colorError: '#ff4d4f',
    borderRadius: layout.borderRadius,
    fontSize: 14,
    controlHeight: 32,
  },
  components: {
    Menu: {
      // v6 口径：itemSelectedBg/itemSelectedColor（colorItemBgSelected 系已废弃）
      itemSelectedBg: palette.blue1,
      itemSelectedColor: palette.blue7,
      activeBarBorderWidth: 0,
    },
    Table: {
      headerBg: neutrals.gray2,
    },
    Layout: {
      headerBg: neutrals.gray1,
    },
  },
}
