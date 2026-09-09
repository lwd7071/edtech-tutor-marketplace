'use client';

import React from 'react';
import { RoleGuard } from '@/shared/components/guards/RoleGuard';
import WorkspaceLayout from '@/shared/components/layout/WorkspaceLayout';

export default function StudentLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <RoleGuard allowedRoles={['STUDENT']}>
      <WorkspaceLayout role="STUDENT">{children}</WorkspaceLayout>
    </RoleGuard>
  );
}
