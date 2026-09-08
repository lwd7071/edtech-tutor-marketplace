'use client';

import React from 'react';
import { TeacherProfileForm } from '@/features/teacher-dashboard/components/TeacherProfileForm';

export default function TeacherProfilePage() {
  return (
    <div style={{ maxWidth: 820, margin: '0 auto' }}>
      <TeacherProfileForm />
    </div>
  );
}
