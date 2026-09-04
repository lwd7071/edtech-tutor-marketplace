'use client';

import { Layout } from 'antd';
import Sidebar from '@/shared/components/layouts/Sidebar';
import TopHeader from '@/shared/components/layouts/TopHeader';
import BottomNavigation from '@/shared/components/layouts/BottomNavigation';

export default function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <Layout style={{ minHeight: '100vh', flexDirection: 'row' }}>
      <Sidebar />
      <Layout style={{ backgroundColor: 'var(--color-background)', minWidth: 0 }}>
        <TopHeader />
        <Layout.Content style={{ padding: 'var(--space-6)', overflow: 'initial' }}>
          <div style={{ maxWidth: 'var(--size-content)', margin: '0 auto' }}>
            {children}
          </div>
        </Layout.Content>
      </Layout>
      <BottomNavigation />
    </Layout>
  );
}
