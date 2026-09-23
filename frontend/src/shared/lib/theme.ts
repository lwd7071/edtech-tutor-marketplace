import type { ThemeConfig } from 'antd';
import { colors } from '@/shared/design-system/tokens';

export const appTheme: ThemeConfig = {
  token: {
    colorPrimary: colors.primary600,
    colorSuccess: colors.success600,
    colorWarning: colors.warning600,
    colorError: colors.error600,
    colorInfo: colors.info600,
    colorBgLayout: colors.background,
    colorBgContainer: colors.surface,
    colorBorder: colors.border,
    colorText: colors.textPrimary,
    colorTextSecondary: colors.textSecondary,
    colorTextTertiary: colors.textTertiary,
    colorTextPlaceholder: colors.textPlaceholder,
    borderRadius: 8, 
    borderRadiusLG: 12, 
    borderRadiusSM: 6,
    fontFamily: 'var(--font-body), -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    fontSize: 15,
    controlHeight: 40, 
    controlHeightSM: 32, 
    controlHeightLG: 48,
    motionDurationMid: '0.18s',
  },
  components: {
    Button: { 
      fontWeight: 600, 
      primaryShadow: 'none',
    },
    Input: {
      activeBorderColor: colors.primary600,
      hoverBorderColor: colors.primary600,
      activeShadow: `0 0 0 3px color-mix(in srgb, ${colors.primary500} 28%, transparent)`,
      errorActiveShadow: `0 0 0 3px color-mix(in srgb, ${colors.error600} 28%, transparent)`,
      paddingBlock: 8,
      paddingInline: 16,
    },
    Select: {
      activeBorderColor: colors.primary600,
      hoverBorderColor: colors.primary600,
    },
    DatePicker: {
      activeBorderColor: colors.primary600,
      hoverBorderColor: colors.primary600,
    },
    Checkbox: {
      colorPrimary: colors.primary600,
      colorPrimaryHover: colors.primaryHover,
    },
    Radio: {
      colorPrimary: colors.primary600,
    },
    Switch: {
      colorPrimary: colors.primary600,
    },
    Tabs: {
      itemColor: colors.textSecondary,
      itemHoverColor: colors.textPrimary,
      itemSelectedColor: colors.primary600,
      inkBarColor: colors.primary600,
    },
    Dropdown: {
      paddingBlock: 8,
      borderRadiusLG: 12,
    },
    Tooltip: {
      borderRadius: 8,
      colorBgSpotlight: 'var(--color-text-primary)', // Darker background for tooltip
    },
    Alert: {
      borderRadiusLG: 8,
    },
    Pagination: {
      itemActiveBg: colors.primary50,
      colorPrimary: colors.primary600,
      colorPrimaryHover: colors.primaryHover,
    },
    Card: { 
      paddingLG: 24, 
      headerFontSize: 16 
    },
    Table: { 
      headerBg: colors.surfaceSunken,
      headerColor: colors.textSecondary,
      rowHoverBg: colors.surfaceHover,
      cellPaddingBlock: 12 
    },
    Layout: { 
      siderBg: colors.surface,
      headerBg: colors.surface,
      headerHeight: 64 
    },
    Menu: { 
      itemHeight: 40, 
      itemBorderRadius: 8, 
      itemSelectedBg: colors.primary50,
      itemSelectedColor: colors.primary600,
    },
    Modal: { 
      borderRadiusLG: 16 
    },
  },
};
