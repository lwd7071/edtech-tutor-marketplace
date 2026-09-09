'use client';

import React from 'react';
import { TeacherApprovalGuard } from '@/shared/components/guards/TeacherApprovalGuard';
import { TeacherWalletPage } from '@/features/finance';

export default function Page() {
  return (
<TeacherApprovalGuard>
        <TeacherWalletPage />
      </TeacherApprovalGuard>
);
}
