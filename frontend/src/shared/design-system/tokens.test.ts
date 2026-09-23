import { colorCss, colors } from './tokens';
import { appTheme } from '@/shared/lib/theme';

describe('shared color source', () => {
  it('maps the brand and surfaces to CSS and Ant Design from one palette', () => {
    expect(colorCss).toContain(`--color-primary-600:${colors.primary600}`);
    expect(colorCss).toContain(`--color-background:${colors.background}`);
    expect(colorCss).toContain(`--color-error-600:${colors.error600}`);
    expect(appTheme.token?.colorPrimary).toBe(colors.primary600);
    expect(appTheme.token?.colorBgLayout).toBe(colors.background);
    expect(appTheme.token?.colorError).toBe(colors.error600);
  });
});
