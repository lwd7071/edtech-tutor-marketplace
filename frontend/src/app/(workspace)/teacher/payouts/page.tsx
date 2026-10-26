'use client';

import React from 'react';
import { TeacherApprovalGuard } from '@/shared/components/guards/TeacherApprovalGuard';
import { TeacherPayoutsPage } from '@/features/finance';

export default function Page() {
  return (
    <TeacherApprovalGuard>
      <TeacherPayoutsPage />
      </TeacherApprovalGuard>
  );
}
