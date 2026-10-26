'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { Alert, Button, Empty, Pagination, Skeleton, Tabs } from 'antd';
import { StudentPackageStatus } from '../types';
import { useStudentPackages } from '../hooks/useStudentPackages';
import { StudentPackageCard } from './StudentPackageCard';

const tabItems = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'ACTIVE', label: 'Đang hoạt động' },
  { key: 'COMPLETED', label: 'Đã hoàn thành' },
  { key: 'LOCKED_EXPIRED', label: 'Hết hạn' },
];

export const StudentPackageList: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(9);
  const statusFilter: StudentPackageStatus | undefined =
    activeTab === 'ALL' ? undefined : (activeTab as StudentPackageStatus);
  const { data, isLoading, isError, refetch } = useStudentPackages(statusFilter, page, pageSize);
  const packages = data?.data ?? [];
  const total = data?.meta?.totalElements ?? packages.length;

  const changeTab = (key: string) => {
    setActiveTab(key);
    setPage(0);
  };

  return (
    <section className="tm-student-package-list" aria-label="Danh sách gói học">
      <div className="tm-student-package-filters">
        <Tabs
          activeKey={activeTab}
          items={tabItems}
          onChange={changeTab}
          aria-label="Lọc gói học theo trạng thái"
        />
      </div>

      {isLoading ? (
        <div className="tm-student-package-loading" role="status" aria-live="polite" aria-busy="true">
          <span>Đang tải danh sách gói học</span>
          <Skeleton active title={false} paragraph={{ rows: 3 }} />
          <Skeleton active title={false} paragraph={{ rows: 3 }} />
        </div>
      ) : isError ? (
        <Alert
          className="tm-student-package-error"
          type="error"
          showIcon
          title="Chưa tải được danh sách gói học"
          description="Thử tải lại để xem trạng thái và số buổi của các gói đã đăng ký."
          action={<Button onClick={() => void refetch()}>Thử lại</Button>}
        />
      ) : packages.length === 0 ? (
        <div className="tm-student-package-empty">
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={activeTab === 'ALL' ? 'Bạn chưa có gói học nào.' : 'Không có gói học trong trạng thái này.'}
          />
          {activeTab === 'ALL' ? (
            <Link className="tm-student-package-primary-link" href="/subjects">Tìm gia sư</Link>
          ) : (
            <Button type="link" onClick={() => changeTab('ALL')}>Xem tất cả gói học</Button>
          )}
        </div>
      ) : (
        <>
          <p className="tm-student-package-result-count" aria-live="polite">
            Tổng số: {total} gói học
          </p>
          <ol className="tm-student-package-rows">
            {packages.map((pkg) => <li key={pkg.id}><StudentPackageCard packageData={pkg} /></li>)}
          </ol>
          {total > pageSize && (
            <div className="tm-student-package-pagination">
              <Pagination
                current={page + 1}
                pageSize={pageSize}
                total={total}
                onChange={(nextPage, nextPageSize) => {
                  setPage(nextPage - 1);
                  setPageSize(nextPageSize);
                }}
                showSizeChanger={false}
                showTotal={(count, range) => `${range[0]}–${range[1]} trong ${count} gói`}
              />
            </div>
          )}
        </>
      )}
    </section>
  );
};
