'use client';

import React from 'react';
import { RoleGuard } from '@/shared/components/guards/RoleGuard';
import { StudentAppLayout } from '@/shared/components/layout/StudentAppLayout';

export default function StudentLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <RoleGuard allowedRoles={['STUDENT']}>
      <StudentAppLayout>{children}</StudentAppLayout>
    </RoleGuard>
  );
}
