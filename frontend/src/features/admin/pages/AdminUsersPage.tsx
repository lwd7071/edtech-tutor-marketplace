'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Alert, Button, Empty, Input, Pagination, Select, Skeleton, Tag } from 'antd';
import { useAdminUsers } from '../hooks/useAdminApprovals';
import type { AdminUserView } from '../types';
import { UserModerationModal } from '../components/UserModerationModal';
import { formatVietnamDateTime } from '@/shared/lib/vietnamTime';
import styles from './AdminUsersPage.module.css';

type SortField = 'createdAt' | 'fullName' | 'email' | 'role' | 'status' | 'lastLoginAt';
type SortDirection = 'asc' | 'desc';

const sortOptions: Array<{ value: SortField; label: string }> = [
  { value: 'createdAt', label: 'Ngày tạo' },
  { value: 'fullName', label: 'Họ tên' },
  { value: 'email', label: 'Email' },
  { value: 'role', label: 'Vai trò' },
  { value: 'status', label: 'Trạng thái' },
  { value: 'lastLoginAt', label: 'Lần đăng nhập cuối' },
];

function UserLoading() {
  return <div className={styles.loading} aria-label="Đang tải danh sách người dùng">
    {[0, 1, 2, 3].map(row => <div className={styles.loadingRow} key={row}><Skeleton active title={{ width: '32%' }} paragraph={{ rows: 2 }} /></div>)}
  </div>;
}

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
  const users = query.data?.data ?? [];
  const total = query.data?.meta?.totalElements ?? 0;

  const submitSearch = (value: string) => {
    setKeyword(value.trim());
    setPage(0);
  };

  const selectSortField = (field: SortField) => {
    setSortField(field);
    setPage(0);
  };

  const toggleSortDirection = () => {
    setSortDirection(direction => direction === 'desc' ? 'asc' : 'desc');
    setPage(0);
  };

  return <div className={styles.page}>
    <header className={styles.heading}>
      <h1>Quản lý người dùng</h1>
      <p>Tìm học viên, gia sư và quản lý trạng thái tài khoản.</p>
    </header>

    <section className={styles.directory} aria-label="Danh sách học viên và gia sư">
      <div className={styles.filters}>
        <Input.Search
          className={styles.search}
          aria-label="Tìm người dùng"
          placeholder="Tìm theo tên hoặc email"
          value={draftKeyword}
          onChange={event => {
            setDraftKeyword(event.target.value);
            if (!event.target.value) submitSearch('');
          }}
          onSearch={submitSearch}
          allowClear
        />
        <Select
          className={styles.filter}
          aria-label="Lọc vai trò"
          allowClear
          placeholder="Tất cả vai trò"
          value={role}
          onChange={value => { setRole(value); setPage(0); }}
          options={[{ value: 'STUDENT', label: 'Học viên' }, { value: 'TEACHER', label: 'Gia sư' }]}
        />
        <Select
          className={styles.filter}
          aria-label="Lọc trạng thái"
          allowClear
          placeholder="Tất cả trạng thái"
          value={status}
          onChange={value => { setStatus(value); setPage(0); }}
          options={[{ value: 'ACTIVE', label: 'Đang hoạt động' }, { value: 'LOCKED', label: 'Đã khóa' }]}
        />
      </div>

      <div className={styles.sortControls} aria-label="Sắp xếp danh sách">
        <span>Sắp xếp theo</span>
        <Select
          aria-label="Sắp xếp theo"
          value={sortField}
          onChange={selectSortField}
          options={sortOptions}
        />
        <Button aria-label="Đổi chiều sắp xếp" aria-pressed={sortDirection === 'asc'} onClick={toggleSortDirection}>
          {sortDirection === 'desc' ? 'Giảm dần' : 'Tăng dần'}
        </Button>
      </div>

      {query.isError ? <Alert
        className={styles.error}
        type="error"
        showIcon
        title="Chưa tải được danh sách người dùng"
        action={<Button onClick={() => void query.refetch()}>Thử lại</Button>}
      /> : query.isLoading ? <UserLoading /> : users.length === 0 ? <div className={styles.empty}>
        <Empty description="Không tìm thấy người dùng phù hợp." />
      </div> : <>
        <div className={styles.listHeader} aria-hidden="true">
          <span>Người dùng</span><span>Vai trò</span><span>Trạng thái</span><span>Hoạt động</span><span>Thao tác</span>
        </div>
        <ul className={styles.users}>
          {users.map(user => <li key={user.id}>
            <article className={styles.userRow}>
              <div className={styles.identity}>
                <h2>{user.fullName}</h2>
                <a href={`mailto:${user.email}`}>{user.email}</a>
              </div>
              <p className={styles.role}>{user.role === 'TEACHER' ? 'Gia sư' : 'Học viên'}</p>
              <div className={styles.status}>
                <Tag color={user.status === 'ACTIVE' ? 'success' : 'error'}>
                  {user.status === 'ACTIVE' ? 'Đang hoạt động' : 'Đã khóa'}
                </Tag>
              </div>
              <dl className={styles.activity}>
                <div><dt>Ngày tạo</dt><dd>{formatVietnamDateTime(user.createdAt)}</dd></div>
                <div><dt>Lần đăng nhập cuối</dt><dd>{user.lastLoginAt ? formatVietnamDateTime(user.lastLoginAt) : 'Chưa đăng nhập'}</dd></div>
              </dl>
              <div className={styles.actions}>
                <Button danger={user.status === 'ACTIVE'} onClick={() => setModerating(user)}>
                  {user.status === 'ACTIVE' ? 'Khóa' : 'Mở khóa'}
                </Button>
                <Link href={`/admin/audit-logs?targetType=USER&targetId=${encodeURIComponent(user.id)}`}>Lịch sử khóa/mở khóa</Link>
              </div>
            </article>
          </li>)}
        </ul>
        <Pagination
          className={styles.pagination}
          current={page + 1}
          pageSize={size}
          total={total}
          showSizeChanger
          pageSizeOptions={[20, 50, 100]}
          showTotal={count => `${count} người dùng`}
          onChange={(nextPage, nextSize) => {
            if (nextSize !== size) {
              setSize(nextSize);
              setPage(0);
              return;
            }
            setPage(nextPage - 1);
          }}
        />
      </>}
    </section>

    <UserModerationModal
      open={!!moderating}
      user={moderating ? { ...moderating, currentStatus: moderating.status } : null}
      onClose={() => setModerating(null)}
    />
  </div>;
}
