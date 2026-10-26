'use client';

import React, { useState } from 'react';
import { Button, Empty, Pagination, Skeleton, Tabs, Tag } from 'antd';
import { TeacherApprovalSnapshot } from '../types';
import { useTeacherApprovals } from '../hooks/useAdminApprovals';
import { TeacherDetailDrawer } from './TeacherDetailDrawer';
import styles from './TeacherApprovalTable.module.css';

const statusLabels: Record<string, string> = {
  PENDING_APPROVAL: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Đã từ chối',
};

function deliveryModes(teacher: TeacherApprovalSnapshot) {
  return [teacher.supportsOnline && 'Online', teacher.supportsOffline && 'Trực tiếp'].filter(Boolean).join(', ') || 'Chưa cập nhật';
}

function statusTag(status: string) {
  if (status === 'APPROVED') return <Tag color="success">Đã duyệt</Tag>;
  if (status === 'REJECTED') return <Tag color="error">Đã từ chối</Tag>;
  return <Tag color="warning">Chờ duyệt</Tag>;
}

export function TeacherApprovalTable() {
  const [selectedStatus, setSelectedStatus] = useState('PENDING_APPROVAL');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [selectedTeacher, setSelectedTeacher] = useState<TeacherApprovalSnapshot | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const query = useTeacherApprovals(selectedStatus, page, pageSize);
  const teachers = query.data?.data ?? [];
  const total = query.data?.meta?.totalElements ?? teachers.length;

  const closeDetails = () => {
    setDrawerOpen(false);
    setSelectedTeacher(null);
  };

  return <section className={styles.queue} aria-label="Danh sách hồ sơ gia sư">
    <div className={styles.queueHeader}>
      <h2>Hồ sơ đăng ký</h2>
      <Tabs
        activeKey={selectedStatus}
        onChange={key => { setSelectedStatus(key); setPage(0); }}
        items={Object.entries(statusLabels).map(([key, label]) => ({ key, label }))}
      />
    </div>

    {query.isError ? <div className={styles.error} role="alert">
      <div><strong>Chưa tải được danh sách hồ sơ.</strong><p>Thử tải lại để tiếp tục xét duyệt.</p></div>
      <Button onClick={() => void query.refetch()}>Tải lại</Button>
    </div> : query.isLoading ? <div className={styles.loading} aria-label="Đang tải hồ sơ gia sư">
      <Skeleton active title={{ width: 220 }} paragraph={{ rows: 3 }} />
      <Skeleton active title={{ width: 220 }} paragraph={{ rows: 3 }} />
    </div> : teachers.length === 0 ? <div className={styles.empty}>
      <Empty description={`Không có hồ sơ ${statusLabels[selectedStatus]?.toLowerCase() ?? 'nào'} ở trang này.`} />
    </div> : <>
      <ul className={styles.list}>
        {teachers.map(teacher => <li key={teacher.teacherProfileId}>
          <article className={styles.teacherRow}>
            <div className={styles.identity}>
              <h3>{teacher.fullName || 'Chưa cập nhật'}</h3>
              <a href={`mailto:${teacher.email}`}>{teacher.email || 'Chưa cập nhật email'}</a>
            </div>
            <dl className={styles.summary}>
              <div><dt>Hình thức dạy</dt><dd>{deliveryModes(teacher)}</dd></div>
              <div><dt>Kinh nghiệm</dt><dd>{teacher.yearsOfExperience ? `${teacher.yearsOfExperience} năm` : 'Chưa cập nhật'}</dd></div>
              <div><dt>Giấy tờ</dt><dd>{teacher.documents?.length ?? 0} tài liệu</dd></div>
            </dl>
            <div className={styles.rowEnd}>
              {statusTag(teacher.status)}
              <Button type="link" onClick={() => { setSelectedTeacher(teacher); setDrawerOpen(true); }}>Xem hồ sơ</Button>
            </div>
          </article>
        </li>)}
      </ul>
      <Pagination
        className={styles.pagination}
        current={page + 1}
        pageSize={pageSize}
        total={total}
        showSizeChanger
        pageSizeOptions={[10, 20, 50]}
        showTotal={count => `${count} hồ sơ`}
        onChange={(current, size) => { setPage(current - 1); setPageSize(size); }}
      />
    </>}

    <TeacherDetailDrawer open={drawerOpen} teacher={selectedTeacher} onClose={closeDetails} />
  </section>;
}
