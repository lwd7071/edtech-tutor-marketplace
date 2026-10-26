'use client';

import React, { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Button, Skeleton } from 'antd';
import { message } from 'antd';
import { useStudentPackageDetail } from '../hooks/useStudentPackages';
import { StudentPackageDetailView } from '../components/StudentPackageDetailView';
import { ActionLink, PageActions } from '@/shared/components/navigation/NavigationLinks';
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
  const { data, isLoading, isError, refetch } = useStudentPackageDetail(id);
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
      <section className="tm-student-package-detail-state" role="status" aria-live="polite" aria-busy="true">
        <p>Đang tải thông tin gói học</p>
        <Skeleton active title={{ width: '55%' }} paragraph={{ rows: 4 }} />
        <Skeleton active title={{ width: '40%' }} paragraph={{ rows: 3 }} />
      </section>
    );
  }

  if (isError || !packageData) {
    return (
      <section className="tm-student-package-detail-state" aria-labelledby="tm-student-package-detail-error-title">
        <div>
          <h1 id="tm-student-package-detail-error-title">Không tìm thấy thông tin gói học hoặc bạn không có quyền xem</h1>
          <p>Kiểm tra lại danh sách gói học của bạn hoặc thử tải lại thông tin.</p>
        </div>
        <PageActions>
          {isError && <Button onClick={() => void refetch()}>Thử lại</Button>}
          <ActionLink href="/student/packages">Xem danh sách gói học</ActionLink>
        </PageActions>
      </section>
    );
  }

  return (
    <div className="tm-student-package-detail-screen" role="region" aria-label="Chi tiết gói học">
      <StudentPackageDetailView
        className="tm-student-package-detail-content"
        packageData={packageData}
        onRefundRequest={() => setRefundOpen(true)}
        onExtensionRequest={() => setExtensionOpen(true)}
      />

      <CreateRefundModal
        open={refundOpen}
        packageId={packageData.id}
        packageVersion={packageData.version}
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
        packageVersion={packageData.version}
        packageName={packageData.packageName}
        currentExpiryDate={packageData.expiresAt}
        loading={createExtensionMutation.isPending}
        onCancel={() => setExtensionOpen(false)}
        onSubmit={handleExtensionSubmit}
      />
    </div>
  );
};
