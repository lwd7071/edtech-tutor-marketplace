/** The restored Tutor Match palette. CSS and Ant Design both derive from this map. */
export const colors = {
  primary50: '#F0FDFA', primary100: '#CCFBF1', primary500: '#14B8A6',
  primary600: '#0F766E', primary700: '#115E59', primary800: '#0B4D48', primary900: '#042F2E',
  primaryHover: '#0D645D',
  accent50: '#FFFBEB', accent500: '#B45309',
  background: '#FBFAF8', surface: '#FFFFFF', surfaceSunken: '#F5F3EF',
  surfaceHover: '#F7F6F3', border: '#E7E3DC', borderStrong: '#D3CEC4',
  textPrimary: '#1C1917', textSecondary: '#57534E', textTertiary: '#8A837B',
  textPlaceholder: '#A8A29E', textInverse: '#FFFFFF', textDisabled: '#B9B3AB',
  disabledBg: '#F1EFEA', success600: '#15803D', successBg: '#F0FDF4',
  warning600: '#B45309', warningBg: '#FFFBEB', error600: '#B91C1C',
  errorBg: '#FEF2F2', info600: '#1D4ED8', infoBg: '#EFF6FF',
} as const;

const cssNames: Record<keyof typeof colors, string> = {
  primary50: '--color-primary-50', primary100: '--color-primary-100', primary500: '--color-primary-500',
  primary600: '--color-primary-600', primary700: '--color-primary-700', primary800: '--color-primary-800', primary900: '--color-primary-900',
  primaryHover: '--color-primary-hover', accent50: '--color-accent-50', accent500: '--color-accent-500',
  background: '--color-background', surface: '--color-surface', surfaceSunken: '--color-surface-sunken',
  surfaceHover: '--color-surface-hover', border: '--color-border', borderStrong: '--color-border-strong',
  textPrimary: '--color-text-primary', textSecondary: '--color-text-secondary', textTertiary: '--color-text-tertiary',
  textPlaceholder: '--color-text-placeholder', textInverse: '--color-text-inverse', textDisabled: '--color-text-disabled',
  disabledBg: '--color-disabled-bg', success600: '--color-success-600', successBg: '--color-success-bg',
  warning600: '--color-warning-600', warningBg: '--color-warning-bg', error600: '--color-error-600',
  errorBg: '--color-error-bg', info600: '--color-info-600', infoBg: '--color-info-bg',
};

export const colorCss = `:root{${(Object.keys(colors) as (keyof typeof colors)[])
  .map(key => `${cssNames[key]}:${colors[key]}`).join(';')}}`;
