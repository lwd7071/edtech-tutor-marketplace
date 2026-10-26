'use client';

import React from 'react';
import { TeacherApprovalGuard } from '@/shared/components/guards/TeacherApprovalGuard';
import { TeacherBankAccountsPage } from '@/features/finance';

export default function Page() {
  return (
    <TeacherApprovalGuard>
      <TeacherBankAccountsPage />
      </TeacherApprovalGuard>
  );
}
