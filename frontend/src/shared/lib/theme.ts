import type { ThemeConfig } from 'antd';

export const appTheme: ThemeConfig = {
  token: {
    colorPrimary: '#0F766E', 
    colorSuccess: '#15803D', 
    colorWarning: '#B45309',
    colorError: '#B91C1C', 
    colorInfo: '#1D4ED8',
    colorBgLayout: '#FBFAF8', 
    colorBgContainer: '#FFFFFF', 
    colorBorder: '#E7E3DC',
    colorText: '#1C1917', 
    colorTextSecondary: '#57534E', 
    colorTextTertiary: '#8A837B',
    colorTextPlaceholder: '#A8A29E',
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
      activeBorderColor: '#0F766E',
      hoverBorderColor: '#0F766E',
      activeShadow: '0 0 0 3px rgba(20,184,166,0.28)',
      errorActiveShadow: '0 0 0 3px rgba(185,28,28,0.28)',
      paddingBlock: 8,
      paddingInline: 16,
    },
    Select: {
      activeBorderColor: '#0F766E',
      hoverBorderColor: '#0F766E',
    },
    DatePicker: {
      activeBorderColor: '#0F766E',
      hoverBorderColor: '#0F766E',
    },
    Checkbox: {
      colorPrimary: '#0F766E',
      colorPrimaryHover: '#0D645D',
    },
    Radio: {
      colorPrimary: '#0F766E',
    },
    Switch: {
      colorPrimary: '#0F766E',
    },
    Tabs: {
      itemColor: '#57534E',
      itemHoverColor: '#1C1917',
      itemSelectedColor: '#0F766E',
      inkBarColor: '#0F766E',
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
      itemActiveBg: '#F0FDFA',
      colorPrimary: '#0F766E',
      colorPrimaryHover: '#0D645D',
    },
    Card: { 
      paddingLG: 24, 
      headerFontSize: 16 
    },
    Table: { 
      headerBg: '#F5F3EF', 
      headerColor: '#57534E', 
      rowHoverBg: '#F7F6F3', 
      cellPaddingBlock: 12 
    },
    Layout: { 
      siderBg: '#FFFFFF', 
      headerBg: '#FFFFFF', 
      headerHeight: 64 
    },
    Menu: { 
      itemHeight: 40, 
      itemBorderRadius: 8, 
      itemSelectedBg: '#F0FDFA', 
      itemSelectedColor: '#0F766E' 
    },
    Modal: { 
      borderRadiusLG: 16 
    },
  },
};
