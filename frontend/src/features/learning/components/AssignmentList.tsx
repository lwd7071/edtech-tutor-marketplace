'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { Alert, Button, Pagination, Skeleton, Tag } from 'antd';
import { formatVietnamDateTime } from '@/shared/lib/vietnamTime';
import { learningApi } from '../api/learningApi';
import { learningKeys } from '../data/learningKeys';
import type { TeacherAssignmentListItem } from '../types';
import StudentAssignmentListLegacy from './StudentAssignmentListLegacy';
import styles from './AssignmentList.module.css';

type AssignmentStatus = TeacherAssignmentListItem['status'];

const assignmentStatusLabels: Record<AssignmentStatus, string> = {
  DRAFT: 'Bản nháp',
  PUBLISHED: 'Đang giao',
  CLOSED: 'Đã đóng',
};

function TeacherAssignmentRow({ assignment }: { assignment: TeacherAssignmentListItem }) {
  const context = [assignment.studentName, assignment.subjectName].filter(Boolean).join(' · ');

  return (
    <li className={styles.listItem}>
      <article className={styles.assignmentRow}>
        <div className={styles.assignmentMain}>
          <div className={styles.assignmentHeading}>
            <h2>{assignment.title}</h2>
            <Tag className={styles.status}>{assignmentStatusLabels[assignment.status]}</Tag>
          </div>
          {context ? <p className={styles.context}>{context}</p> : null}
        </div>

        <div className={styles.metric}>
          <span>Bài đã nộp</span>
          <strong>{assignment.submittedCount} / {assignment.totalStudents}</strong>
        </div>

        <div className={styles.deadline}>
          <span>Hạn nộp</span>
          <strong>
            {assignment.dueAt
              ? formatVietnamDateTime(assignment.dueAt)
              : 'Chưa có hạn nộp'}
          </strong>
        </div>

        <Link className={styles.openLink} href={`/teacher/assignments/${assignment.id}`}>
          Xem chi tiết
        </Link>
      </article>
    </li>
  );
}

function TeacherAssignments({
  assignments,
  total,
  page,
  onPageChange,
}: {
  assignments: TeacherAssignmentListItem[];
  total: number;
  page: number;
  onPageChange: (page: number) => void;
}) {
  if (!assignments.length) {
    return (
      <section className={styles.empty}>
        <p className={styles.eyebrow}>Danh sách đang trống</p>
        <h2>Chưa có bài tập nào</h2>
        <p>Bài tập đã giao sẽ xuất hiện tại đây để bạn theo dõi hạn nộp và bài đã nộp.</p>
        <Link className={styles.emptyLink} href="/teacher/assignments/new">
          Giao bài tập
        </Link>
      </section>
    );
  }

  return (
    <>
      <ol className={styles.list} aria-label="Danh sách bài tập đã giao">
        {assignments.map(assignment => (
          <TeacherAssignmentRow key={assignment.id} assignment={assignment} />
        ))}
      </ol>
      {total > 12 ? (
        <div className={styles.pagination}>
          <Pagination
            className={styles.paginationControl}
            current={page + 1}
            total={total}
            pageSize={12}
            showSizeChanger={false}
            onChange={current => onPageChange(current - 1)}
          />
        </div>
      ) : null}
    </>
  );
}

function TeacherAssignmentList() {
  const [page, setPage] = useState(0);
  const query = useQuery({
    queryKey: learningKeys.assignments('teacher', page),
    queryFn: () => learningApi.getTeacherAssignments(page, 12),
  });

  return (
    <section className={styles.page}>
      <header className={styles.pageHeading}>
        <div>
          <p className={styles.eyebrow}>Không gian gia sư</p>
          <h1>Bài tập đã giao</h1>
          <p>Theo dõi người nhận, hạn nộp và số bài học viên đã gửi.</p>
        </div>
        <Link className={styles.createLink} href="/teacher/assignments/new">
          Giao bài tập
        </Link>
      </header>

      {query.isLoading ? (
        <section className={styles.loading} role="status" aria-label="Đang tải bài tập">
            <Skeleton className={styles.loadingSkeleton} active paragraph={{ rows: 3 }} />
          <Skeleton className={styles.loadingSkeleton} active paragraph={{ rows: 3 }} />
        </section>
      ) : query.isError ? (
        <section className={styles.errorState}>
          <Alert
            className={styles.errorAlert}
            type="error"
            title="Chưa tải được bài tập"
            description="Kiểm tra kết nối rồi thử tải lại danh sách."
            action={<Button className={styles.retryButton} onClick={() => query.refetch()}>Thử lại</Button>}
          />
        </section>
      ) : (
        <>
          <div className={styles.resultsHeading}>
            <h2>Bài tập</h2>
            <span>{query.data?.meta?.totalElements ?? 0} bài</span>
          </div>
          <TeacherAssignments
            assignments={query.data?.data ?? []}
            total={query.data?.meta?.totalElements ?? 0}
            page={page}
            onPageChange={setPage}
          />
        </>
      )}
    </section>
  );
}

export default function AssignmentList({ role }: { role: 'student' | 'teacher' }) {
  return role === 'teacher' ? <TeacherAssignmentList /> : <StudentAssignmentListLegacy />;
}
