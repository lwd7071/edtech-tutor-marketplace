'use client';

import React, { useState } from 'react';
import { Tabs, Empty, Pagination, Spin, Row, Col, Typography } from 'antd';
import { StudentPackageStatus } from '../types';
import { useStudentPackages } from '../hooks/useStudentPackages';
import { StudentPackageCard } from './StudentPackageCard';

export const StudentPackageList: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(9);

  const statusFilter: StudentPackageStatus | undefined =
    activeTab === 'ALL' ? undefined : (activeTab as StudentPackageStatus);

  const { data, isLoading } = useStudentPackages(statusFilter, page, pageSize);

  const packages = data?.data || [];
  const total = data?.meta?.totalElements || packages.length;

  const tabItems = [
    { key: 'ALL', label: 'Tất cả' },
    { key: 'ACTIVE', label: 'Đang hoạt động' },
    { key: 'COMPLETED', label: 'Đã hoàn thành' },
    { key: 'LOCKED_EXPIRED', label: 'Hết hạn' },
  ];

  return (
    <div>
      <Tabs
        activeKey={activeTab}
        items={tabItems}
        onChange={(key) => {
          setActiveTab(key);
          setPage(0);
        }}
        style={{ marginBottom: 20 }}
      />

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '60px 0' }}>
          <Spin size="large" />
        </div>
      ) : packages.length === 0 ? (
        <Empty
          description="Chưa có gói học nào trong danh mục này"
          style={{ padding: '40px 0' }}
        />
      ) : (
        <>
          <Row gutter={[20, 20]}>
            {packages.map((pkg) => (
              <Col xs={24} sm={12} lg={8} key={pkg.id}>
                <StudentPackageCard packageData={pkg} />
              </Col>
            ))}
          </Row>

          <div style={{ display: 'flex', justifyContent: 'center', marginTop: 32 }}>
            <Pagination
              current={page + 1}
              pageSize={pageSize}
              total={total}
              onChange={(p, ps) => {
                setPage(p - 1);
                setPageSize(ps);
              }}
              showSizeChanger={false}
            />
          </div>
        </>
      )}
    </div>
  );
};
