'use client';

import React, { ReactNode } from 'react';
import { useAuthStore } from '@/features/auth';
import { Alert, Button, Space } from 'antd';
import Link from 'next/link';

interface TeacherApprovalGuardProps {
  children: ReactNode;
  fallback?: ReactNode;
}

/**
 * TeacherApprovalGuard: Kiểm tra trạng thái phê duyệt của tài khoản Giáo viên
 * Nếu giáo viên đang ở trạng thái DRAFT hoặc PENDING, chặn các tính năng yêu cầu hồ sơ đã duyệt
 */
export function TeacherApprovalGuard({ children, fallback }: TeacherApprovalGuardProps) {
  const { user, isAuthenticated } = useAuthStore();

  if (isAuthenticated && user?.role === 'TEACHER' && user?.status !== 'APPROVED') {
    if (fallback) return <>{fallback}</>;

    return (
      <div style={{ padding: 'var(--space-8)', maxWidth: 640, margin: 'var(--space-12) auto' }}>
        <Alert
          type="info"
          showIcon
          title="Hồ sơ đang chờ phê duyệt"
          description={
            <div style={{ width: '100%', marginTop: 'var(--space-3)', display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
              <div>
                Hồ sơ giáo viên của bạn đang được ban quản trị xét duyệt. Các tính năng tạo gói học,
                quản lý lịch dạy và rút tiền sẽ được mở sau khi hồ sơ được phê duyệt thành công.
              </div>
              <Link href="/teacher/profile">
                <Button type="primary">Kiểm tra trạng thái hồ sơ</Button>
              </Link>
            </div>
          }
        />
      </div>
    );
  }

  return <>{children}</>;
}
