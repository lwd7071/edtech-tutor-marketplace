'use client';

import React from 'react';
import { useParams } from 'next/navigation';
import { Spin, Empty, Button } from 'antd';
import { useStudentPackageDetail } from '../hooks/useStudentPackages';
import { StudentPackageDetailView } from '../components/StudentPackageDetailView';
import { ActionLink, PageActions } from '@/shared/components/navigation/NavigationLinks';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { message } from 'antd';
import {
  CreateRefundModal,
  CreateExtensionModal,
  useCreateRefund,
  useCreateExtension,
  CreateRefundRequest,
  CreateExtensionRequest,
} from '@/features/finance';

export const StudentPackageDetailPage: React.FC = () => {
  const params = useParams();
  const router = useRouter();
  const id = params?.id as string;

  const [refundOpen, setRefundOpen] = useState(false);
  const [extensionOpen, setExtensionOpen] = useState(false);

  const { data, isLoading, isError } = useStudentPackageDetail(id);
  const createRefundMutation = useCreateRefund();
  const createExtensionMutation = useCreateExtension();

  const packageData = data?.data;

  const handleRefundSubmit = async (values: CreateRefundRequest) => {
    try {
      await createRefundMutation.mutateAsync(values);
      message.success('Gửi yêu cầu hoàn tiền thành công');
      setRefundOpen(false);
      router.push('/student/requests');
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Không thể gửi yêu cầu hoàn tiền');
    }
  };

  const handleExtensionSubmit = async (values: CreateExtensionRequest) => {
    try {
      await createExtensionMutation.mutateAsync(values);
      message.success('Gửi yêu cầu gia hạn thành công');
      setExtensionOpen(false);
      router.push('/student/requests');
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Không thể gửi yêu cầu gia hạn');
    }
  };

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '100px 0' }}>
        <Spin size="large" description="Đang tải thông tin gói học..." />
      </div>
    );
  }

  if (isError || !packageData) {
    return (
      <div style={{ textAlign: 'center', padding: '80px 0' }}>
        <Empty description="Không tìm thấy thông tin gói học hoặc bạn không có quyền xem" />
        <PageActions centered><ActionLink href="/student/packages">Xem danh sách gói học</ActionLink></PageActions>
      </div>
    );
  }

  return (
    <div style={{ padding: 'var(--space-6, 24px) var(--space-4, 16px)' }}>
      <StudentPackageDetailView
        packageData={packageData}
        onRefundRequest={() => setRefundOpen(true)}
        onExtensionRequest={() => setExtensionOpen(true)}
      />

      <CreateRefundModal
        open={refundOpen}
        packageId={packageData.id}
        packageName={packageData.packageName}
        remainingSessions={packageData.remainingSessions}
        estimatedPricePerSession={packageData.totalSessions ? Math.round(packageData.purchasePriceVnd / packageData.totalSessions) : 0}
        loading={createRefundMutation.isPending}
        onCancel={() => setRefundOpen(false)}
        onSubmit={handleRefundSubmit}
      />

      <CreateExtensionModal
        open={extensionOpen}
        packageId={packageData.id}
        packageName={packageData.packageName}
        currentExpiryDate={packageData.expiresAt}
        loading={createExtensionMutation.isPending}
        onCancel={() => setExtensionOpen(false)}
        onSubmit={handleExtensionSubmit}
      />
    </div>
  );
};
