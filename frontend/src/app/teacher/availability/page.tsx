'use client';

import React from 'react';
import { AvailabilityEditor } from '@/features/teacher-dashboard/components/AvailabilityEditor';

export default function TeacherAvailabilityPage() {
  return (
    <div style={{ maxWidth: 1024, margin: '0 auto' }}>
      <AvailabilityEditor />
    </div>
  );
}
