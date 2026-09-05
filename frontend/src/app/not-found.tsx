'use client';

import React from 'react';
import { ErrorState } from '@/shared/components/feedback/ErrorState';
import { useRouter } from 'next/navigation';

export default function NotFound() {
  const router = useRouter();
  
  return (
    <div style={{ height: 'calc(100vh - 64px)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <ErrorState 
        variant="page"
        title="404 - Không tìm thấy trang"
        message="Trang bạn đang tìm kiếm không tồn tại hoặc đã bị di chuyển."
        onRetry={() => router.push('/')}
        actionText="Quay lại Trang chủ"
      />
    </div>
  );
}
