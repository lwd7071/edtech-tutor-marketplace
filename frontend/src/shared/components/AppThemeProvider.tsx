'use client';

import { ConfigProvider, App } from 'antd';
import { AntdRegistry } from '@ant-design/nextjs-registry';
import { appTheme } from '@/shared/lib/theme';
import React from 'react';

/**
 * AppThemeProvider is a deep module that encapsulates:
 * 1. Ant Design's CSS-in-JS Next.js registry (prevents FOUC in SSR).
 * 2. Ant Design's ConfigProvider with our custom tokens.
 * 3. Ant Design's App wrapper to provide context-aware message, notification, and modal.
 * 
 * This creates a clear seam between our application layout and the 
 * underlying UI library infrastructure.
 */
export function AppThemeProvider({ children }: { children: React.ReactNode }) {
  return (
    <AntdRegistry>
      <ConfigProvider theme={appTheme}>
        <App>
          {children}
        </App>
      </ConfigProvider>
    </AntdRegistry>
  );
}
