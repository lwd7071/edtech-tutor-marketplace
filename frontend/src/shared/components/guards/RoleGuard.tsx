'use client';

import React, { ReactNode } from 'react';
import { useAuthStore, UserRole } from '@/features/auth';
import { Alert, Button, Space } from 'antd';
import Link from 'next/link';

interface RoleGuardProps {
  children: ReactNode;
  allowedRoles: UserRole[];
  fallback?: ReactNode;
}

/**
 * RoleGuard: Kiểm tra phân quyền theo vai trò (STUDENT, TEACHER, ADMIN)
 * Nếu không đúng vai trò, hiển thị giao diện 403 thân thiện
 */
export function RoleGuard({ children, allowedRoles, fallback }: RoleGuardProps) {
  const { user, isAuthenticated } = useAuthStore();

  if (!isAuthenticated || !user || !allowedRoles.includes(user.role)) {
    if (fallback) return <>{fallback}</>;

    return (
      <div style={{ padding: 'var(--space-8)', maxWidth: 600, margin: 'var(--space-12) auto' }}>
        <Alert
          type="warning"
          showIcon
          title="403 - Bạn không có quyền truy cập trang này"
          description={
            <div style={{ width: '100%', marginTop: 'var(--space-3)', display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
              <div>Khu vực này chỉ dành cho tài khoản có vai trò: {allowedRoles.join(', ')}.</div>
              <Link href="/">
                <Button type="primary">Quay về Trang chủ</Button>
              </Link>
            </div>
          }
        />
      </div>
    );
  }

  return <>{children}</>;
}
