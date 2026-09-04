'use client';

import React from 'react';
import { RoleGuard } from '@/shared/components/guards/RoleGuard';
import { StudentRequestsPage } from '@/features/finance';

export default function Page() {
  return (
    <RoleGuard allowedRoles={['STUDENT']}>
      <StudentRequestsPage />
    </RoleGuard>
  );
}
