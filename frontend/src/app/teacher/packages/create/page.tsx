'use client';

import React from 'react';
import { useRouter } from 'next/navigation';
import { PackageForm } from '@/features/teacher-dashboard/components/PackageForm';

export default function CreatePackagePage() {
  const router = useRouter();

  const handleSave = () => {
    router.push('/teacher/packages');
  };

  const handleCancel = () => {
    router.push('/teacher/packages');
  };

  return (
    <div style={{ maxWidth: 820, margin: '0 auto' }}>
      <PackageForm mode="create" onSave={handleSave} onCancel={handleCancel} />
    </div>
  );
}
