'use client';

import React from 'react';
import { RoleGuard } from '@/shared/components/guards/RoleGuard';
import { TeacherApprovalGuard } from '@/shared/components/guards/TeacherApprovalGuard';
import { TeacherWalletPage } from '@/features/finance';

export default function Page() {
  return (
    <RoleGuard allowedRoles={['TEACHER']}>
      <TeacherApprovalGuard>
        <TeacherWalletPage />
      </TeacherApprovalGuard>
    </RoleGuard>
  );
}
