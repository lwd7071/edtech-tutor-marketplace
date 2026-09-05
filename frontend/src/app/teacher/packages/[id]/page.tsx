'use client';

import React, { use } from 'react';
import { useRouter } from 'next/navigation';
import { PackageForm } from '@/features/teacher-dashboard/components/PackageForm';

export default function EditPackagePage({ params }: { params: Promise<{ id: string }> }) {
  const router = useRouter();
  const { id } = use(params);

  const handleSave = () => {
    router.push('/teacher/packages');
  };

  const handleCancel = () => {
    router.push('/teacher/packages');
  };

  return (
    <div className="max-w-3xl mx-auto">
      <PackageForm mode="edit" packageId={id} onSave={handleSave} onCancel={handleCancel} />
    </div>
  );
}
