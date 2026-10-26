'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, App, Button, Empty, Pagination, Skeleton, Tag } from 'antd';
import { teacherApi } from '@/shared/api/teacher';
import { isConcurrentModification, parseApiError } from '@/shared/backend';
import { teacherDashboardApi } from '../api/teacherDashboardApi';
import { teacherDashboardKeys } from '../data/teacherDashboardKeys';
import styles from './PackageList.module.css';

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Bản nháp',
  ACTIVE: 'Đang mở bán',
  INACTIVE: 'Ngừng bán',
};

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

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div className={styles.heading}>
          <h1>Gói học của bạn</h1>
          <p>Giá trọn gói, số buổi và thời hạn rõ ràng cho học viên.</p>
        </div>
        <Button className={styles.createButton} type="primary" disabled={profile.data?.approvalStatus !== 'APPROVED'} onClick={onCreate}>
          Tạo gói học
        </Button>
      </header>

      {profile.data?.approvalStatus !== 'APPROVED' && (
        <Alert className={styles.approvalNotice} type="info" showIcon message="Hồ sơ cần được duyệt trước khi tạo gói học" />
      )}

      {packages.isLoading ? (
        <section className={styles.loading} role="status" aria-label="Đang tải gói học">
          <span>Đang tải danh sách gói học...</span>
          <Skeleton active paragraph={{ rows: 3 }} />
          <Skeleton active paragraph={{ rows: 3 }} />
        </section>
      ) : packages.isError ? (
        <Alert className={styles.error} type="error" showIcon message="Chưa tải được gói học" action={<Button onClick={() => packages.refetch()}>Thử lại</Button>} />
      ) : packages.data?.data.length ? (
        <>
          <section className={styles.listSection} aria-labelledby="packages-list-title">
            <div className={styles.listHeading}>
              <h2 id="packages-list-title">Danh sách gói</h2>
              <span>{packages.data.meta?.totalElements ?? packages.data.data.length} gói</span>
            </div>
            <ul className={styles.packageList}>
              {packages.data.data.map(pkg => (
                <li key={pkg.id} className={styles.packageRow}>
                  <div className={styles.packageIdentity}>
                    <div className={styles.packageTitle}>
                      <h3>{pkg.name}</h3>
                      <Tag className={`${styles.status} ${styles[`status_${pkg.status}` as keyof typeof styles] ?? ''}`}>
                        {STATUS_LABELS[pkg.status] || pkg.status}
                      </Tag>
                    </div>
                    <p className={styles.subject}>{pkg.subjectName}</p>
                    {pkg.description && <p className={styles.description}>{pkg.description}</p>}
                  </div>
                  <dl className={styles.terms}>
                    <div><dt>Số buổi</dt><dd>{pkg.totalSessions} buổi</dd></div>
                    <div><dt>Thời lượng</dt><dd>{pkg.sessionDurationMinutes} phút/buổi</dd></div>
                    <div><dt>Thời hạn</dt><dd>{pkg.durationDays} ngày</dd></div>
                  </dl>
                  <div className={styles.priceBlock}>
                    <span>Giá gói</span>
                    <strong>{pkg.priceVnd.toLocaleString('vi-VN')}đ</strong>
                  </div>
                  <div className={styles.actions}>
                    <Button onClick={() => onEdit(pkg.id)}>Chỉnh sửa</Button>
                    <Button loading={busy === pkg.id} onClick={() => changeStatus(pkg.id, pkg.status, pkg.version)}>
                      {pkg.status === 'ACTIVE' ? 'Ngừng bán' : 'Mở bán'}
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          </section>
          <Pagination
            className={styles.pagination}
            current={page + 1}
            total={packages.data.meta?.totalElements || 0}
            pageSize={12}
            showSizeChanger={false}
            onChange={next => setPage(next - 1)}
          />
        </>
      ) : (
        <section className={styles.empty} aria-labelledby="packages-empty-title">
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={null} />
          <h2 id="packages-empty-title">Chưa có gói học</h2>
          <p>Hoàn thiện hồ sơ để bắt đầu tạo gói.</p>
        </section>
      )}
    </main>
  );
}
