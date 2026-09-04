'use client';

import React from 'react';
import { RoleGuard } from '@/shared/components/guards/RoleGuard';
import { AdminAuditLogsPage } from '@/features/admin';

export default function Page() {
  return (
    <RoleGuard allowedRoles={['ADMIN']}>
      <AdminAuditLogsPage />
    </RoleGuard>
  );
}
