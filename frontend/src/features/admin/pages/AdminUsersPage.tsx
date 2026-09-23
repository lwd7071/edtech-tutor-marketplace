'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { Alert, Button, Input, Select, Table, Tag, Typography } from 'antd';
import type { TableProps } from 'antd';
import { useAdminUsers } from '../hooks/useAdminApprovals';
import type { AdminUserView } from '../types';
import { UserModerationModal } from '../components/UserModerationModal';
import { formatVietnamDateTime } from '@/shared/lib/vietnamTime';

type SortField = 'createdAt' | 'fullName' | 'email' | 'role' | 'status' | 'lastLoginAt';
type SortDirection = 'asc' | 'desc';

export function AdminUsersPage() {
  const [draftKeyword, setDraftKeyword] = useState('');
  const [keyword, setKeyword] = useState('');
  const [role, setRole] = useState<'STUDENT' | 'TEACHER'>();
  const [status, setStatus] = useState<'ACTIVE' | 'LOCKED'>();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [sortField, setSortField] = useState<SortField>('createdAt');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
  const [moderating, setModerating] = useState<AdminUserView | null>(null);
  const sort = `${sortField},${sortDirection}`;
  const query = useAdminUsers({ keyword: keyword || undefined, role, status, page, size, sort });
  const submitSearch = (value: string) => { setKeyword(value.trim()); setPage(0); };

  const columns = useMemo<TableProps<AdminUserView>['columns']>(() => {
    const sortable = (field: SortField) => ({ sorter: true as const, sortOrder: sortField === field ? (sortDirection === 'asc' ? 'ascend' as const : 'descend' as const) : null });
    return [
      { title: 'Họ tên', dataIndex: 'fullName', key: 'fullName', ...sortable('fullName') },
      { title: 'Email', dataIndex: 'email', key: 'email', ...sortable('email') },
      { title: 'Vai trò', dataIndex: 'role', key: 'role', ...sortable('role'), render: (value: string) => value === 'TEACHER' ? 'Gia sư' : 'Học viên' },
      { title: 'Trạng thái', dataIndex: 'status', key: 'status', ...sortable('status'), render: (value: string) => <Tag color={value === 'ACTIVE' ? 'green' : 'red'}>{value === 'ACTIVE' ? 'Đang hoạt động' : 'Đã khóa'}</Tag> },
      { title: 'Ngày tạo', dataIndex: 'createdAt', key: 'createdAt', ...sortable('createdAt'), render: (value: string) => formatVietnamDateTime(value) },
      { title: 'Lần đăng nhập cuối', dataIndex: 'lastLoginAt', key: 'lastLoginAt', ...sortable('lastLoginAt'), render: (value: string | null) => value ? formatVietnamDateTime(value) : '—' },
      { title: 'Thao tác', key: 'actions', render: (_: unknown, user: AdminUserView) => <div className="tm-inline">
        <Button onClick={() => setModerating(user)}>{user.status === 'ACTIVE' ? 'Khóa' : 'Mở khóa'}</Button>
        <Link href={`/admin/audit-logs?targetType=USER&targetId=${encodeURIComponent(user.id)}`}>Lịch sử khóa/mở khóa</Link>
      </div> },
    ];
  }, [sortField, sortDirection]);

  const handleTableChange: TableProps<AdminUserView>['onChange'] = (pagination, _filters, sorter) => {
    const sorted = Array.isArray(sorter) ? sorter[0] : sorter;
    if (pagination.pageSize && pagination.pageSize !== size) {
      setSize(pagination.pageSize);
      setPage(0);
      return;
    }
    if (sorted?.field && sorted.order) {
      const nextField = String(sorted.field) as SortField;
      const nextDirection = sorted.order === 'ascend' ? 'asc' : 'desc';
      if (nextField !== sortField || nextDirection !== sortDirection) {
        setSortField(nextField);
        setSortDirection(nextDirection);
        setPage(0);
        return;
      }
    }
    if (pagination.current) setPage(pagination.current - 1);
  };

  return <div className="tm-stack">
    <div>
      <Typography.Title level={2}>Quản lý người dùng</Typography.Title>
      <Typography.Text type="secondary">Tìm học viên, gia sư và quản lý trạng thái tài khoản.</Typography.Text>
    </div>
    <div className="tm-toolbar">
      <Input.Search style={{ flex: '1 1 280px', minWidth: 240, maxWidth: 440 }} aria-label="Tìm người dùng" placeholder="Tìm theo tên hoặc email" value={draftKeyword}
        onChange={event => { setDraftKeyword(event.target.value); if (!event.target.value) submitSearch(''); }}
        onSearch={submitSearch} allowClear />
      <Select style={{ width: 180 }} aria-label="Lọc vai trò" allowClear placeholder="Tất cả vai trò" value={role} onChange={value => { setRole(value); setPage(0); }}
      >
        <Select.Option value="STUDENT">Học viên</Select.Option>
        <Select.Option value="TEACHER">Gia sư</Select.Option>
      </Select>
      <Select style={{ width: 180 }} aria-label="Lọc trạng thái" allowClear placeholder="Tất cả trạng thái" value={status} onChange={value => { setStatus(value); setPage(0); }}
      >
        <Select.Option value="ACTIVE">Đang hoạt động</Select.Option>
        <Select.Option value="LOCKED">Đã khóa</Select.Option>
      </Select>
    </div>
    {query.isError && <Alert type="error" title="Chưa tải được danh sách người dùng" action={<Button onClick={() => query.refetch()}>Thử lại</Button>} />}
    {!query.isError && <Table<AdminUserView> rowKey="id" columns={columns} dataSource={query.data?.data ?? []} loading={query.isLoading}
      onChange={handleTableChange} pagination={{ current: page + 1, pageSize: size, total: query.data?.meta?.totalElements ?? 0,
        showSizeChanger: true, pageSizeOptions: [20, 50, 100], showTotal: total => `${total} người dùng` }} scroll={{ x: 1000 }} />}
    <UserModerationModal open={!!moderating} user={moderating ? { ...moderating, currentStatus: moderating.status } : null}
      onClose={() => setModerating(null)} />
  </div>;
}
