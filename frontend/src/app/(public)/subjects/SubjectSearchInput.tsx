'use client';

import React from 'react';
import { useRouter } from 'next/navigation';
import DebouncedSearch from '@/shared/components/ui/DebouncedSearch';

export default function SubjectSearchInput({ defaultValue }: { defaultValue?: string }) {
  const router = useRouter();

  return (
    <DebouncedSearch
      placeholder="Tìm kiếm môn học..."
      defaultValue={defaultValue}
      onSearch={(val) => {
        if (val) {
          router.push(`/subjects?keyword=${encodeURIComponent(val)}`);
        } else {
          router.push('/subjects');
        }
      }}
    />
  );
}
