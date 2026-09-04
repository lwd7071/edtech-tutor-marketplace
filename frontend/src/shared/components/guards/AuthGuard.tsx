'use client';

import React, { ReactNode, useEffect } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuthStore } from '@/features/auth';

interface AuthGuardProps {
  children: ReactNode;
  fallback?: ReactNode;
}

/**
 * AuthGuard: Bảo vệ các trang yêu cầu đăng nhập
 * Nếu chưa đăng nhập, tự động chuyển hướng về /auth/login kèm tham số redirect
 */
export function AuthGuard({ children, fallback }: AuthGuardProps) {
  const { isAuthenticated } = useAuthStore();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isAuthenticated) {
      const redirectUrl = pathname ? `/auth/login?redirect=${encodeURIComponent(pathname)}` : '/auth/login';
      router.push(redirectUrl);
    }
  }, [isAuthenticated, router, pathname]);

  if (!isAuthenticated) {
    return fallback ? <>{fallback}</> : null;
  }

  return <>{children}</>;
}
