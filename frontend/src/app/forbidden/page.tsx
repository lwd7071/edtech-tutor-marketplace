'use client';
import React from 'react';
import { ErrorState } from '@/shared/components/feedback/ErrorState';
import { useRouter } from 'next/navigation';

export default function ForbiddenPage() {
  const router = useRouter();
  
  return (
    <div style={{ height: 'calc(100vh - 64px)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <ErrorState 
        variant="page"
        title="403 - Truy cập bị từ chối"
        message="Bạn không có quyền truy cập vào trang này."
        onRetry={() => router.back()}
        actionText="Quay lại Trang chủ"
      />
    </div>
  );
}
