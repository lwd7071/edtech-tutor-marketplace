'use client';

import { formatVietnamDateTime, toVietnamLocalInput, vietnamLocalInputToIso } from '@/shared/lib/vietnamTime';
import { useParams } from 'next/navigation';
import { useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { Alert, App, Button, Form, Input, Skeleton, Tag } from 'antd';
import { learningApi } from '@/features/learning/api/learningApi';
import { learningKeys } from '@/features/learning/data/learningKeys';
import { isConcurrentModification, parseApiError } from '@/shared/backend';
import type { CreateAssignmentRequest, TeacherAssignmentDetail } from '@/features/learning/types';
import styles from './assignment-detail.module.css';

type DraftValues = { title: string; instructions: string; dueAt: string };

const assignmentStatusLabels = {
  DRAFT: 'Bản nháp',
  PUBLISHED: 'Đã phát hành',
  CLOSED: 'Đã đóng',
} as const;

const submissionStatusLabels = {
  DRAFT: 'Bản nháp',
  SUBMITTED: 'Đã nộp',
  GRADED: 'Đã chấm',
} as const;

function formatDeadline(value: string | null | undefined) {
  return value ? formatVietnamDateTime(value) : 'Chưa đặt';
}

export default function Page() {
  const [busy, setBusy] = useState(false);
  const { message } = App.useApp();
  const { id } = useParams<{ id: string }>();
  const query = useQuery({
    queryKey: learningKeys.teacherAssignment(id),
    queryFn: () => learningApi.getTeacherAssignmentDetail(id),
  });

  async function perform(action: () => Promise<unknown>) {
    setBusy(true);
    try {
      await action();
      await query.refetch();
      message.success('Đã cập nhật bài tập');
    } catch (error) {
      if (isConcurrentModification(error)) {
        message.error('Dữ liệu đã được thay đổi bởi người khác. Vui lòng tải lại.');
        await query.refetch();
      } else {
        message.error(parseApiError(error).message);
      }
    } finally {
      setBusy(false);
    }
  }

  if (query.isLoading) {
    return (
      <section className={styles.page} role="status" aria-label="Đang tải bài tập">
        <div className={styles.loadingHeading} />
        <Skeleton active paragraph={{ rows: 5 }} />
        <Skeleton active paragraph={{ rows: 3 }} />
      </section>
    );
  }

  if (query.isError || !query.data?.data) {
    return (
      <section className={styles.page}>
        <Link className={styles.backLink} href="/teacher/assignments">Danh sách bài tập</Link>
        <Alert
          className={styles.error}
          type="error"
          title="Chưa đọc được bài tập"
          description="Bài tập có thể đã bị xóa hoặc bạn không có quyền xem."
          action={<Button className={styles.retry} onClick={() => query.refetch()}>Thử lại</Button>}
        />
      </section>
    );
  }

  const assignment: TeacherAssignmentDetail = query.data.data;
  const textBlocks = assignment.contentBlocks.filter(block => block.type === 'TEXT' && block.content);
  const submissions = assignment.submissions ?? [];

  function updateDraft(values: DraftValues) {
    const request: CreateAssignmentRequest = {
      studentId: assignment.studentId,
      subjectId: assignment.subjectId,
      title: values.title,
      assignmentType: 'FREEFORM',
      status: 'DRAFT',
      dueAt: vietnamLocalInputToIso(values.dueAt),
      contentBlocks: [
        { type: 'TEXT', content: values.instructions },
        ...assignment.contentBlocks.filter(block => block.type !== 'TEXT'),
      ],
      version: assignment.version,
    };
    return perform(() => learningApi.updateDraft(id, request));
  }

  return (
    <div className={styles.page}>
      <Link className={styles.backLink} href="/teacher/assignments">Danh sách bài tập</Link>

      <header className={styles.header}>
        <div>
          <p className={styles.eyebrow}>Không gian gia sư / Bài tập</p>
          <div className={styles.titleLine}>
            <h1>{assignment.title}</h1>
            <Tag className={styles.status}>{assignmentStatusLabels[assignment.status]}</Tag>
          </div>
          <p className={styles.context}>{assignment.studentName} · {assignment.subjectName}</p>
        </div>
      </header>

      <dl className={styles.facts} aria-label="Thông tin bài tập">
        <div><dt>Học viên</dt><dd>{assignment.studentName}</dd></div>
        <div><dt>Môn học</dt><dd>{assignment.subjectName}</dd></div>
        <div><dt>Hạn nộp</dt><dd>{formatDeadline(assignment.dueAt)}</dd></div>
        <div><dt>Bài đã nộp</dt><dd>{assignment.submittedCount} / {assignment.totalStudents}</dd></div>
      </dl>

      {assignment.status === 'DRAFT' ? (
        <section className={styles.section} aria-labelledby="assignment-edit-title">
          <div className={styles.sectionHeading}>
            <div><p className={styles.eyebrow}>Bước tiếp theo</p><h2 id="assignment-edit-title">Hoàn thiện bài tập</h2></div>
            <p>Kiểm tra nội dung và hạn nộp trước khi phát hành.</p>
          </div>
          <Form<DraftValues>
            key={JSON.stringify(assignment.contentBlocks) + assignment.title + assignment.dueAt}
            className={styles.editForm}
            layout="vertical"
            initialValues={{
              title: assignment.title,
              instructions: assignment.contentBlocks.filter(block => block.type === 'TEXT').map(block => block.content).join('\n\n'),
              dueAt: assignment.dueAt ? toVietnamLocalInput(assignment.dueAt) : undefined,
            }}
            onFinish={updateDraft}
          >
            <Form.Item className={styles.titleField} name="title" label="Tên bài tập" rules={[{ required: true }, { max: 255 }]}>
              <Input autoComplete="off" />
            </Form.Item>
            <Form.Item className={styles.instructionsField} name="instructions" label="Đề bài" rules={[{ required: true }]}>
              <Input.TextArea rows={7} />
            </Form.Item>
            <Form.Item className={styles.deadlineField} name="dueAt" label="Hạn nộp (giờ Việt Nam)" rules={[{ required: true }]}>
              <Input type="datetime-local" />
            </Form.Item>
            <div className={styles.actions}>
              <Button className={styles.saveButton} htmlType="submit" loading={busy}>Lưu chỉnh sửa</Button>
              <Button className={styles.primaryAction} type="primary" loading={busy} onClick={() => void perform(() => learningApi.publishAssignment(id, assignment.version))}>
                Phát hành bản đã lưu
              </Button>
            </div>
          </Form>
        </section>
      ) : (
        <section className={styles.section} aria-labelledby="assignment-prompt-title">
          <div className={styles.sectionHeading}>
            <div><p className={styles.eyebrow}>Nội dung đã giao</p><h2 id="assignment-prompt-title">Đề bài</h2></div>
            <p>Hạn nộp: {formatDeadline(assignment.dueAt)}</p>
          </div>
          {textBlocks.length ? (
            <div className={styles.prompt}>
              {textBlocks.map((block, index) => <p key={`${index}-${block.content}`}>{block.content}</p>)}
            </div>
          ) : (
            <p className={styles.muted}>Chưa có nội dung đề bài.</p>
          )}
          {assignment.status === 'PUBLISHED' ? (
            <div className={styles.publishedAction}>
              <p>Học viên có thể nộp bài cho đến khi bài tập được đóng.</p>
              <Button className={styles.closeButton} loading={busy} onClick={() => void perform(() => learningApi.closeAssignment(id, assignment.version))}>
                Đóng nhận bài
              </Button>
            </div>
          ) : null}
        </section>
      )}

      <section className={styles.submissions} aria-labelledby="assignment-submissions-title">
        <div className={styles.submissionHeading}>
          <div><p className={styles.eyebrow}>Tiến độ</p><h2 id="assignment-submissions-title">Bài nộp</h2></div>
          <span>{submissions.length} bài</span>
        </div>
        {submissions.length ? (
          <ol className={styles.submissionList}>
            {submissions.map(submission => (
              <li key={submission.id}>
                <article className={styles.submissionRow}>
                  <div className={styles.submissionMain}>
                    <h3>{assignment.studentName}</h3>
                    <p>{submission.submittedAt ? formatVietnamDateTime(submission.submittedAt) : 'Chưa gửi bài'}</p>
                  </div>
                  <div className={styles.submissionState}>
                    <span>Trạng thái</span>
                    <Tag className={styles.status}>{submissionStatusLabels[submission.status]}</Tag>
                  </div>
                  <div className={styles.score}>
                    <span>Điểm</span>
                    <strong>{submission.status === 'GRADED' && submission.score !== null ? `${submission.score}/10` : 'Chưa chấm'}</strong>
                  </div>
                  <Link className={styles.submissionLink} href={`/teacher/submissions/${submission.id}`}>Xem và chấm bài</Link>
                </article>
              </li>
            ))}
          </ol>
        ) : (
          <p className={styles.empty}>Học viên chưa nộp bài.</p>
        )}
      </section>
    </div>
  );
}
