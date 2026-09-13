'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, App, Button, Pagination, Skeleton, Tag } from 'antd';
import { teacherApi } from '@/shared/api/teacher';
import { isConcurrentModification, parseApiError } from '@/shared/backend';
import { teacherDashboardApi } from '../api/teacherDashboardApi';
import { teacherDashboardKeys } from '../data/teacherDashboardKeys';

export function PackageList({ onCreate, onEdit }: { onCreate: () => void; onEdit: (id: string) => void }) {
  const [page, setPage] = useState(0);
  const [busy, setBusy] = useState<string | null>(null);
  const { message } = App.useApp();
  const profile = useQuery({ queryKey: teacherDashboardKeys.profile(), queryFn: teacherApi.getProfile });
  const packages = useQuery({
    queryKey: teacherDashboardKeys.packages(page),
    queryFn: () => teacherDashboardApi.getPackages(page, 12),
  });

  const changeStatus = async (id: string, status: string, version: number) => {
    setBusy(id);
    try {
      await teacherApi.updatePackageStatus(id, status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE', version);
      await packages.refetch();
    } catch (error) {
      if (isConcurrentModification(error)) {
        message.error('Dữ liệu gói đã thay đổi, vui lòng tải lại.');
        await packages.refetch();
      } else {
        message.error(parseApiError(error).message);
      }
    } finally {
      setBusy(null);
    }
  };

  return <div className="tm-stack">
    <header className="tm-toolbar">
      <div className="tm-page-heading"><h1>Gói học của bạn</h1><p>Giá trọn gói, số buổi và thời hạn rõ ràng cho học viên.</p></div>
      <Button type="primary" disabled={profile.data?.approvalStatus !== 'APPROVED'} onClick={onCreate}>Tạo gói học</Button>
    </header>
    {profile.data?.approvalStatus !== 'APPROVED' && <Alert type="info" title="Hồ sơ cần được duyệt trước khi tạo gói học" />}
    {packages.isLoading ? <Skeleton active /> : packages.isError
      ? <Alert type="error" title="Chưa tải được gói học" action={<Button onClick={() => packages.refetch()}>Thử lại</Button>} />
      : <>
        <div className="tm-package-grid">{packages.data?.data.map(pkg => <article key={pkg.id} className="tm-panel">
          <Tag>{{ DRAFT: 'Bản nháp', ACTIVE: 'Đang mở bán', INACTIVE: 'Ngừng bán' }[pkg.status] || pkg.status}</Tag>
          <h2>{pkg.name}</h2>
          <p>{pkg.subjectName} · {pkg.totalSessions} buổi · {pkg.sessionDurationMinutes} phút/buổi</p>
          <p>Hạn sử dụng {pkg.durationDays} ngày</p>
          <p className="tm-package-price">{pkg.priceVnd.toLocaleString('vi-VN')}đ</p>
          <div className="tm-inline">
            <Button onClick={() => onEdit(pkg.id)}>Chỉnh sửa</Button>
            <Button loading={busy === pkg.id} onClick={() => changeStatus(pkg.id, pkg.status, pkg.version)}>
              {pkg.status === 'ACTIVE' ? 'Ngừng bán' : 'Mở bán'}
            </Button>
          </div>
        </article>)}</div>
        {!packages.data?.data.length && <div className="tm-panel">Chưa có gói học. Hoàn thiện hồ sơ để bắt đầu tạo gói.</div>}
        <Pagination current={page + 1} total={packages.data?.meta?.totalElements || 0} pageSize={12} showSizeChanger={false} onChange={next => setPage(next - 1)} />
      </>}
  </div>;
}
