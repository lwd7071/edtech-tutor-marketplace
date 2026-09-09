'use client';

import React, { useState } from 'react';
import { Tabs, Tag, Button, Space, Typography, Card } from 'antd';
import { EyeOutlined } from '@ant-design/icons';
import ResponsiveTable from '@/shared/components/ui/ResponsiveTable';
import { TeacherApprovalSnapshot } from '../types';
import { useTeacherApprovals } from '../hooks/useAdminApprovals';
import { TeacherDetailDrawer } from './TeacherDetailDrawer';

export function TeacherApprovalTable() {
  const [selectedStatus, setSelectedStatus] = useState<string>('PENDING_APPROVAL');
  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(10);
  const [selectedTeacher, setSelectedTeacher] = useState<TeacherApprovalSnapshot | null>(null);
  const [drawerOpen, setDrawerOpen] = useState<boolean>(false);

  const { data, isLoading } = useTeacherApprovals(selectedStatus, page, pageSize);

  const teachers = data?.data || [];
  const total = data?.meta?.totalElements || teachers.length;

  const handleOpenDetail = (record: TeacherApprovalSnapshot) => {
    setSelectedTeacher(record);
    setDrawerOpen(true);
  };

  const columns = [
    {
      title: 'Họ và tên',
      dataIndex: 'fullName',
      key: 'fullName',
      render: (text: string, record: TeacherApprovalSnapshot) => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Typography.Text strong>{text || 'Chưa cập nhật'}</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 'var(--text-body-sm)' }}>
            {record.email}
          </Typography.Text>
        </div>
      ),
    },
    {
      title: 'Hình thức dạy',
      key: 'deliveryModes',
      render: (_: unknown, record: TeacherApprovalSnapshot) => [record.supportsOnline && 'Online', record.supportsOffline && 'Trực tiếp'].filter(Boolean).join(', ') || 'Chưa cập nhật',
    },
    {
      title: 'Kinh nghiệm',
      dataIndex: 'yearsOfExperience',
      key: 'yearsOfExperience',
      render: (years: number) => (years ? `${years} năm` : 'Chưa cập nhật'),
    },
    {
      title: 'Chứng chỉ',
      dataIndex: 'documents',
      key: 'documents',
      render: (docs: any[]) => `${docs?.length || 0} tài liệu`,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        if (status === 'APPROVED') return <Tag color="success">Đã duyệt</Tag>;
        if (status === 'REJECTED') return <Tag color="error">Đã từ chối</Tag>;
        return <Tag color="warning">Chờ duyệt</Tag>;
      },
    },
    {
      title: 'Thao tác',
      key: 'action',
      render: (_: any, record: TeacherApprovalSnapshot) => (
        <Button
          type="primary"
          ghost
          icon={<EyeOutlined />}
          onClick={() => handleOpenDetail(record)}
        >
          Xem hồ sơ
        </Button>
      ),
    },
  ];

  return (
    <Card title="Quản lý phê duyệt hồ sơ gia sư" style={{ width: '100%' }}>
      <Tabs
        activeKey={selectedStatus}
        onChange={(key) => {
          setSelectedStatus(key);
          setPage(0);
        }}
        items={[
          { key: 'PENDING_APPROVAL', label: 'Chờ duyệt' },
          { key: 'APPROVED', label: 'Đã duyệt' },
          { key: 'REJECTED', label: 'Đã từ chối' },
        ]}
      />

      <ResponsiveTable
        dataSource={teachers}
        columns={columns}
        rowKey="teacherProfileId"
        loading={isLoading}
        pagination={{
          current: page + 1,
          pageSize,
          total,
          onChange: (p, s) => {
            setPage(p - 1);
            setPageSize(s);
          },
        }}
        mobileCardRender={(record: TeacherApprovalSnapshot) => (
          <Space orientation="vertical" style={{ width: '100%' }}>
            <Typography.Text strong>{record.fullName}</Typography.Text>
            <Typography.Text type="secondary">{record.email}</Typography.Text>
            <div>Hình thức: {[record.supportsOnline && 'Online', record.supportsOffline && 'Trực tiếp'].filter(Boolean).join(', ') || 'Chưa cập nhật'}</div>
            <div>Kinh nghiệm: {record.yearsOfExperience ? `${record.yearsOfExperience} năm` : 'Chưa cập nhật'}</div>
            <Button
              type="primary"
              ghost
              style={{ marginTop: 'var(--space-2)' }}
              onClick={() => handleOpenDetail(record)}
            >
              Xem hồ sơ
            </Button>
          </Space>
        )}
      />

      <TeacherDetailDrawer
        open={drawerOpen}
        teacher={selectedTeacher}
        onClose={() => {
          setDrawerOpen(false);
          setSelectedTeacher(null);
        }}
      />
    </Card>
  );
}
