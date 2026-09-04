'use client';

import React from 'react';
import { useParams } from 'next/navigation';
import { Spin, Empty, Button } from 'antd';
import Link from 'next/link';
import { useStudentPackageDetail } from '../hooks/useStudentPackages';
import { StudentPackageDetailView } from '../components/StudentPackageDetailView';

export const StudentPackageDetailPage: React.FC = () => {
  const params = useParams();
  const id = params?.id as string;

  const { data, isLoading, isError } = useStudentPackageDetail(id);
  const packageData = data?.data;

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '100px 0' }}>
        <Spin size="large" tip="Đang tải thông tin gói học..." />
      </div>
    );
  }

  if (isError || !packageData) {
    return (
      <div style={{ textAlign: 'center', padding: '80px 0' }}>
        <Empty description="Không tìm thấy thông tin gói học hoặc bạn không có quyền xem" />
        <Link href="/student/packages" style={{ marginTop: 16, display: 'inline-block' }}>
          <Button type="primary">Quay lại danh sách</Button>
        </Link>
      </div>
    );
  }

  return (
    <div style={{ padding: 'var(--space-6, 24px) var(--space-4, 16px)' }}>
      <StudentPackageDetailView packageData={packageData} />
    </div>
  );
};
