/** Tutor Match visual system. CSS and Ant Design derive from this single map. */
export const colors = {
  brandYellow: '#FFD43B', actionPrimary: '#FFD43B', actionHover: '#F2C11D', ink: '#171714',
  primary50: '#FFFBEB', primary100: '#FFF0A8', primary500: '#FFD43B',
  primary600: '#171714', primary700: '#11110F', primary800: '#0B0B09', primary900: '#171714',
  primaryHover: '#33322C',
  accent50: '#FFFBEB', accent500: '#FFD43B',
  background: '#F7F6F0', surface: '#FFFEFA', surfaceSunken: '#F1EFE7',
  surfaceHover: '#F8F5E8', border: '#E5E2D8', borderStrong: '#CFCBBE',
  textPrimary: '#171714', textSecondary: '#55544C', textTertiary: '#77766D',
  textPlaceholder: '#A8A29E', textInverse: '#FFFFFF', textDisabled: '#B9B3AB',
  disabledBg: '#F1EFEA', success600: '#15803D', successBg: '#F0FDF4',
  warning600: '#B45309', warningBg: '#FFFBEB', error600: '#B91C1C',
  errorBg: '#FEF2F2', info600: '#1D4ED8', infoBg: '#EFF6FF',
} as const;

const cssNames: Record<keyof typeof colors, string> = {
  brandYellow: '--color-brand-yellow', actionPrimary: '--color-action-primary', actionHover: '--color-action-hover', ink: '--color-ink',
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
