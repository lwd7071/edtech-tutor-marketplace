'use client';

import React from 'react';
import { StudentAssignmentList } from '@/features/learning/components/StudentAssignmentList';

export default function StudentAssignmentsPage() {
  return (
    <div className="max-w-7xl mx-auto py-6">
      <StudentAssignmentList />
    </div>
  );
}
