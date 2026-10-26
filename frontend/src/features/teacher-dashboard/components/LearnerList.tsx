'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { Alert, Button, Pagination, Skeleton } from 'antd';
import { CreateBookingModal } from '@/features/bookings';
import { formatVietnamDate } from '@/shared/lib/vietnamTime';
import { teacherDashboardApi, type LearnerPackage } from '../api/teacherDashboardApi';
import { teacherDashboardKeys } from '../data/teacherDashboardKeys';
import styles from './LearnerList.module.css';

export async function getLearners(page = 0, size = 12, studentId?: string) {
  return teacherDashboardApi.getLearners(page, size, studentId);
}

export default function LearnerList({ studentId }: { studentId?: string }) {
  const [page, setPage] = useState(0);
  const [packageId, setPackageId] = useState<string | null>(null);
  const query = useQuery({
    queryKey: teacherDashboardKeys.learners(page, studentId),
    queryFn: () => getLearners(page, 12, studentId),
  });

  const heading = studentId ? 'Gói học của học viên' : 'Học viên và gói đang học';

  if (query.isLoading) {
    return (
      <main className={styles.page}>
        {studentId && <Link className={styles.backLink} href="/teacher/students">Danh sách học viên</Link>}
        <header className={styles.header}>
          <h1 className={styles.title}>{heading}</h1>
          <p className={styles.description}>Chọn gói để tạo buổi học hoặc giao bài tập.</p>
        </header>
        <div className={styles.loading} role="status" aria-label="Đang tải gói học của học viên">
          <span>Đang tải gói học...</span>
          <Skeleton active paragraph={{ rows: 3 }} />
          <Skeleton active paragraph={{ rows: 3 }} />
        </div>
      </main>
    );
  }

  if (query.isError) {
    return (
      <main className={styles.page}>
        {studentId && <Link className={styles.backLink} href="/teacher/students">Danh sách học viên</Link>}
        <header className={styles.header}>
          <h1 className={styles.title}>{heading}</h1>
          <p className={styles.description}>Chọn gói để tạo buổi học hoặc giao bài tập.</p>
        </header>
        <Alert
          className={styles.error}
          type="error"
          title="Chưa tải được gói học"
          action={<Button onClick={() => query.refetch()}>Thử lại</Button>}
        />
      </main>
    );
  }

  const packages: LearnerPackage[] = query.data?.data ?? [];
  const studentName = packages[0]?.studentName;

  return (
    <main className={styles.page}>
      {studentId && <Link className={styles.backLink} href="/teacher/students">Danh sách học viên</Link>}
      <header className={styles.header}>
        <div>
          <h1 className={styles.title}>{heading}</h1>
          {studentName && <p className={styles.description}>{studentName} · Chọn gói để tạo buổi học hoặc giao bài tập.</p>}
          {!studentName && <p className={styles.description}>Chọn gói để tạo buổi học hoặc giao bài tập.</p>}
        </div>
        {packages.length > 0 && <span className={styles.total}>{query.data?.meta?.totalElements ?? packages.length} gói học</span>}
      </header>

      {packages.length > 0 ? (
        <section aria-labelledby="student-packages-title">
          <h2 id="student-packages-title" className={styles.visuallyHidden}>Danh sách gói học</h2>
          <ul className={styles.list}>
            {packages.map(row => {
              const bookingEnabled = row.studentPackage.status === 'ACTIVE' && row.studentPackage.remainingSessions >= 1;
              return (
                <li key={row.studentPackage.id} className={styles.row}>
                  <div className={styles.package}>
                    <h3>{row.studentPackage.packageName}</h3>
                    <p className={styles.subject}>{row.subjectName}</p>
                  </div>
                  <dl className={styles.progress} aria-label={`Thông tin gói ${row.studentPackage.packageName}`}>
                    <div><dt>Lượt còn</dt><dd>{row.studentPackage.remainingSessions} buổi</dd></div>
                    <div><dt>Hết hạn</dt><dd>{formatVietnamDate(row.studentPackage.expiresAt)}</dd></div>
                  </dl>
                  <div className={styles.actions} role="group" aria-label={`Thao tác với gói ${row.studentPackage.packageName}`}>
                    <Button type="primary" disabled={!bookingEnabled} onClick={() => setPackageId(row.studentPackage.id)}>
                      Tạo buổi học
                    </Button>
                    <Link href={`/teacher/assignments/new?studentId=${row.studentId}&subjectId=${row.studentPackage.subjectId}`}>
                      Giao bài tập
                    </Link>
                  </div>
                </li>
              );
            })}
          </ul>
        </section>
      ) : (
        <section className={styles.empty} aria-labelledby="student-packages-empty-title">
          <h2 id="student-packages-empty-title">Học viên chưa có gói học.</h2>
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
