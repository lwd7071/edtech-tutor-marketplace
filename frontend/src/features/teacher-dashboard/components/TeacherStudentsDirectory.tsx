'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { Alert, Button, Pagination, Skeleton } from 'antd';
import { CreateBookingModal } from '@/features/bookings';
import { formatVietnamDate } from '@/shared/lib/vietnamTime';
import { teacherDashboardApi } from '../api/teacherDashboardApi';
import { teacherDashboardKeys } from '../data/teacherDashboardKeys';
import styles from './TeacherStudentsDirectory.module.css';

export default function TeacherStudentsDirectory() {
  const [page, setPage] = useState(0);
  const [packageId, setPackageId] = useState<string | null>(null);
  const query = useQuery({
    queryKey: teacherDashboardKeys.learners(page),
    queryFn: () => teacherDashboardApi.getLearners(page, 12),
  });

  if (query.isLoading) {
    return (
      <main className={styles.page}>
        <header className={styles.header}>
          <h1>Học viên và gói đang học</h1>
          <p>Chọn gói thuộc học viên để tạo buổi học hoặc giao bài.</p>
        </header>
        <div className={styles.loading} role="status" aria-label="Đang tải danh sách học viên">
          <span>Đang tải học viên...</span>
          <Skeleton active paragraph={{ rows: 4 }} />
          <Skeleton active paragraph={{ rows: 4 }} />
        </div>
      </main>
    );
  }

  if (query.isError) {
    return (
      <main className={styles.page}>
        <header className={styles.header}>
          <h1>Học viên và gói đang học</h1>
          <p>Chọn gói thuộc học viên để tạo buổi học hoặc giao bài.</p>
        </header>
        <Alert
          className={styles.error}
          type="error"
          title="Chưa tải được học viên"
          action={<Button onClick={() => query.refetch()}>Thử lại</Button>}
        />
      </main>
    );
  }

  const learners = query.data?.data ?? [];

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div>
          <h1>Học viên và gói đang học</h1>
          <p>Chọn gói thuộc học viên để tạo buổi học hoặc giao bài.</p>
        </div>
        <span className={styles.total}>{query.data?.meta?.totalElements ?? learners.length} quan hệ học tập</span>
      </header>

      {learners.length > 0 ? (
        <section aria-labelledby="learner-list-title">
          <h2 id="learner-list-title" className={styles.visuallyHidden}>Danh sách học viên và gói học</h2>
          <ul className={styles.list}>
            {learners.map(row => {
              const bookingEnabled = row.studentPackage.status === 'ACTIVE' && row.studentPackage.remainingSessions >= 1;
              return (
                <li key={row.studentPackage.id} className={styles.row}>
                  <section className={styles.identity} aria-label={`Thông tin học viên ${row.studentName}`}>
                    <h3><Link className={styles.studentLink} href={`/teacher/students/${row.studentId}`}>{row.studentName}</Link></h3>
                    <p className={styles.context}>{row.subjectName}<span aria-hidden="true"> · </span>{row.studentPackage.packageName}</p>
                  </section>
                  <dl className={styles.progress} aria-label={`Thông tin gói học của ${row.studentName}`}>
                    <div><dt>Lượt còn</dt><dd>{row.studentPackage.remainingSessions} buổi</dd></div>
                    <div><dt>Hết hạn</dt><dd>{formatVietnamDate(row.studentPackage.expiresAt)}</dd></div>
                  </dl>
                  <div className={styles.actions} role="group" aria-label={`Thao tác với gói ${row.studentPackage.packageName}`}>
                    <Button className={styles.bookingButton} type="primary" disabled={!bookingEnabled} onClick={() => setPackageId(row.studentPackage.id)}>
                      Tạo buổi học
                    </Button>
                    <Link className={styles.assignmentLink} href={`/teacher/assignments/new?studentId=${row.studentId}&subjectId=${row.studentPackage.subjectId}`}>
                      Giao bài tập
                    </Link>
                  </div>
                </li>
              );
            })}
          </ul>
        </section>
      ) : (
        <section className={styles.empty} aria-labelledby="learner-empty-title">
          <h2 id="learner-empty-title">Chưa có gói học của học viên trong tài khoản.</h2>
        </section>
      )}

      <Pagination
        className={styles.pagination}
        current={page + 1}
        total={query.data?.meta?.totalElements || 0}
        pageSize={12}
        showSizeChanger={false}
        onChange={nextPage => setPage(nextPage - 1)}
      />

      <CreateBookingModal
        open={!!packageId}
        studentPackageId={packageId || ''}
        onClose={() => {
          setPackageId(null);
          query.refetch();
        }}
      />
    </main>
  );
}
