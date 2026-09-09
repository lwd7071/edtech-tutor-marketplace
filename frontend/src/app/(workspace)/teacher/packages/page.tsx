'use client';

import React from 'react';
import { useRouter } from 'next/navigation';
import { PackageList } from '@/features/teacher-dashboard/components/PackageList';

export default function TeacherPackagesPage() {
  const router = useRouter();

  const handleCreate = () => {
    router.push('/teacher/packages/create');
  };

  const handleEdit = (id: string) => {
    router.push(`/teacher/packages/${id}`);
  };

  return (
    <div style={{ maxWidth: 1024, margin: '0 auto' }}>
      <PackageList onCreate={handleCreate} onEdit={handleEdit} />
    </div>
  );
}
