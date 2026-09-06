'use client';

import React from 'react';
import { useParams } from 'next/navigation';
import { StudentAssignmentDetail } from '@/features/learning/components/StudentAssignmentDetail';

export default function StudentAssignmentDetailPage() {
  const params = useParams();
  const id = params?.id as string;

  return (
    <div className="py-6">
      <StudentAssignmentDetail assignmentId={id} />
    </div>
  );
}
