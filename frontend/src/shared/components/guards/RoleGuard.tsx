'use client';

import { useEffect } from 'react';
import Link from 'next/link';
import { Button, Result } from 'antd';
import { usePathname, useRouter } from 'next/navigation';
import { useAuthStore, type UserRole } from '@/features/auth';

interface RoleGuardProps {
  children: React.ReactNode;
  allowedRoles: UserRole[];
  fallback?: React.ReactNode;
}

export function RoleGuard({ children, allowedRoles, fallback }: RoleGuardProps) {
  const { status, user, isAuthenticated } = useAuthStore();
  const router = useRouter();
  const pathname = usePathname();
  const hasAllowedRole = Boolean(user && allowedRoles.includes(user.role));

  useEffect(() => {
    if (status === 'anonymous') {
      router.replace(`/auth/login?redirect=${encodeURIComponent(pathname)}`);
    } else if (status === 'authenticated' && !hasAllowedRole && !fallback) {
      router.replace('/forbidden');
    }
  }, [fallback, hasAllowedRole, pathname, router, status]);

  if (status === 'booting') return null;
  if (!isAuthenticated || !user) return <Result title="Đang chuyển đến đăng nhập" />;
  if (!hasAllowedRole) return fallback ?? <Result status="403" title="Đang chuyển đến trang báo lỗi quyền truy cập" />;
  if (['LOCKED', 'DISABLED'].includes(user.status)) {
    return <Result status="warning" title="Tài khoản hiện không thể sử dụng" subTitle="Vui lòng liên hệ đơn vị vận hành để kiểm tra trạng thái tài khoản." />;
  }
  if (user.status === 'PENDING_VERIFICATION') {
    return <Result status="info" title="Xác minh email để tiếp tục" extra={<Link href="/auth/verify-email"><Button>Xác minh email</Button></Link>} />;
  }
  return <>{children}</>;
}
