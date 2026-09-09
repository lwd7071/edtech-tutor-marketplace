'use client';

import React from 'react';
import { TeacherStatsOverview } from '@/features/ranking/components/TeacherStatsOverview';

export default function TeacherStatsPage() {
  return (
    <div style={{ maxWidth: 1120, margin: '0 auto' }}>
      <TeacherStatsOverview />
    </div>
  );
}
