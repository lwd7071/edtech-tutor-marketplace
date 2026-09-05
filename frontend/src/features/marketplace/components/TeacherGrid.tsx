'use client';

import React from 'react';
import { Row, Col } from 'antd';
import TeacherCard from '@/shared/components/data-display/TeacherCard';
import { EmptyState } from '@/shared/components/feedback/EmptyState';
import { ErrorState } from '@/shared/components/feedback/ErrorState';
import { Skeleton } from '@/shared/components/feedback/Skeleton';

interface TeacherGridProps {
  teachers: any[];
  isLoading?: boolean;
  isError?: boolean;
  onRetry?: () => void;
}

export default function TeacherGrid({ teachers, isLoading, isError, onRetry }: TeacherGridProps) {
  if (isError) {
    return <ErrorState actionText="Tải lại" onRetry={onRetry} />;
  }

  if (isLoading) {
    return (
      <Row gutter={[16, 16]}>
        {Array.from({ length: 3 }).map((_, i) => (
          <Col xs={24} sm={24} md={12} lg={8} key={i}>
            <Skeleton variant="card" />
          </Col>
        ))}
      </Row>
    );
  }

  if (!teachers || teachers.length === 0) {
    return (
      <EmptyState
        title="Không tìm thấy giáo viên"
        description="Vui lòng thử lại với tiêu chí khác"
      />
    );
  }

  return (
    <Row gutter={[16, 16]}>
      {teachers.map((teacher) => (
        <Col xs={24} sm={24} md={12} lg={8} key={teacher.id}>
          <TeacherCard 
            id={teacher.id}
            name={teacher.user?.fullName || teacher.name}
            avatarUrl={teacher.user?.avatarUrl || teacher.avatarUrl}
            isVerified={teacher.isVerified}
            subjects={teacher.subjects?.map((s: any) => s.subjectName || s)}
            rating={teacher.rating}
            reviewCount={teacher.reviewCount}
            lowestPrice={teacher.lowestPrice}
          />
        </Col>
      ))}
    </Row>
  );
}
