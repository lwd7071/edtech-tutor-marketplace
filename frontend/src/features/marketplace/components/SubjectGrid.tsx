'use client';

import React from 'react';
import { Row, Col } from 'antd';
import SubjectCard from '@/shared/components/data-display/SubjectCard';
import { EmptyState } from '@/shared/components/feedback/EmptyState';
import { ErrorState } from '@/shared/components/feedback/ErrorState';
import { Skeleton } from '@/shared/components/feedback/Skeleton';

interface SubjectGridProps {
  subjects: any[];
  isLoading?: boolean;
  isError?: boolean;
  onRetry?: () => void;
}

export default function SubjectGrid({ subjects, isLoading, isError, onRetry }: SubjectGridProps) {
  if (isError) {
    return <ErrorState actionText="Tải lại" onRetry={onRetry} />;
  }

  if (isLoading) {
    return (
      <Row gutter={[16, 16]}>
        {Array.from({ length: 4 }).map((_, i) => (
          <Col xs={24} sm={12} md={8} lg={6} key={i}>
            <Skeleton variant="card" />
          </Col>
        ))}
      </Row>
    );
  }

  if (!subjects || subjects.length === 0) {
    return (
      <EmptyState
        title="Không tìm thấy môn học"
        description="Vui lòng thử lại với từ khóa khác"
      />
    );
  }

  return (
    <Row gutter={[16, 16]}>
      {subjects.map((subject) => (
        <Col xs={24} sm={12} md={8} lg={6} key={subject.id}>
          <SubjectCard 
            id={subject.id}
            name={subject.name}
            description={subject.description}
            imageUrl={subject.imageUrl}
            teacherCount={subject.teacherCount}
          />
        </Col>
      ))}
    </Row>
  );
}
