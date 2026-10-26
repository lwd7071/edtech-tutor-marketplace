'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { Alert, Button, Pagination, Skeleton, Tag } from 'antd';
import { formatVietnamDateTime } from '@/shared/lib/vietnamTime';
import { learningApi } from '../api/learningApi';
import { learningKeys } from '../data/learningKeys';

/** Compatibility view for AssignmentList's existing student role prop. */
export default function StudentAssignmentListLegacy() {
  const [page, setPage] = useState(0);
  const query = useQuery({
    queryKey: learningKeys.assignments('student', page),
    queryFn: () => learningApi.getStudentAssignments(page, 12, 'TODO'),
  });

  return (
    <div className="tm-stack">
      <header className="tm-toolbar">
        <div className="tm-page-heading">
          <h1>Bài tập của tôi</h1>
          <p>Xem đề bài, thời hạn và kết quả trong trang chi tiết.</p>
        </div>
      </header>
      {query.isLoading ? (
        <Skeleton active />
      ) : query.isError ? (
        <Alert
          type="error"
          title="Chưa tải được bài tập"
          action={<Button onClick={() => query.refetch()}>Thử lại</Button>}
        />
      ) : (
        <>
          <div className="tm-package-grid">
            {query.data?.data.map(assignment => (
              <Link
                href={`/student/assignments/${assignment.id}`}
                className="tm-panel tm-dashboard-link"
                key={assignment.id}
              >
                <Tag>
                  {({
                    DRAFT: 'Bản nháp',
                    PUBLISHED: 'Đang giao',
                    CLOSED: 'Đã đóng',
                  } as const)[assignment.status]}
                </Tag>
                <h2>{assignment.title}</h2>
                {assignment.studentName ? (
                  <p>{assignment.studentName} · {assignment.subjectName}</p>
                ) : null}
                <p>
                  {assignment.dueAt
                    ? `Hạn nộp: ${formatVietnamDateTime(assignment.dueAt)}`
                    : 'Chưa có hạn nộp'}
                </p>
                <strong>Xem bài tập →</strong>
              </Link>
            ))}
          </div>
          {!query.data?.data.length ? (
            <section className="tm-panel">Chưa có bài tập.</section>
          ) : null}
          <Pagination
            current={page + 1}
            total={query.data?.meta?.totalElements || 0}
            pageSize={12}
            showSizeChanger={false}
            onChange={current => setPage(current - 1)}
          />
        </>
      )}
    </div>
  );
}
