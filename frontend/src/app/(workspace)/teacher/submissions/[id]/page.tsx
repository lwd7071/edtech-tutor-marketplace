'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, App, Button, Form, Input, InputNumber, Skeleton, Tag } from 'antd';
import { learningApi } from '@/features/learning/api/learningApi';
import { learningKeys } from '@/features/learning/data/learningKeys';
import type { GradeSubmissionRequest, SubmissionDetail } from '@/features/learning/types';
import { isConcurrentModification, parseApiError } from '@/shared/backend';
import { formatVietnamDateTime } from '@/shared/lib/vietnamTime';
import styles from './submission-detail.module.css';

type GradeValues = Pick<GradeSubmissionRequest, 'score' | 'feedbackText'>;

const statusLabels: Record<SubmissionDetail['status'], string> = {
  DRAFT: 'Bản nháp',
  SUBMITTED: 'Chờ chấm',
  GRADED: 'Đã chấm',
};

function displayTime(value: string | null) {
  return value ? formatVietnamDateTime(value) : 'Chưa ghi nhận';
}

export default function Page() {
  const { id } = useParams<{ id: string }>();
  const [busy, setBusy] = useState(false);
  const { message } = App.useApp();
  const query = useQuery({
    queryKey: learningKeys.submission(id),
    queryFn: () => learningApi.getSubmissionDetail(id),
  });

  async function saveGrade(values: GradeValues) {
    const submission = query.data?.data;
    if (!submission || submission.status !== 'SUBMITTED') return;

    setBusy(true);
    try {
      await learningApi.gradeSubmission(id, { ...values, version: submission.version });
      await query.refetch();
      message.success('Đã lưu điểm');
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
      <section className={styles.page} role="status" aria-label="Đang tải bài nộp">
        <div className={styles.loadingHeading} />
        <Skeleton active paragraph={{ rows: 5 }} />
      </section>
    );
  }

  if (query.isError || !query.data?.data) {
    return (
      <section className={styles.page}>
        <Link className={styles.backLink} href="/teacher/assignments">Bài tập liên quan</Link>
        <Alert
          className={styles.error}
          type="error"
          title="Chưa đọc được bài nộp"
          description="Bài nộp có thể đã bị xóa hoặc bạn không có quyền xem."
          action={<Button className={styles.retry} onClick={() => query.refetch()}>Thử lại</Button>}
        />
      </section>
    );
  }

  const submission: SubmissionDetail = query.data.data;
  const textBlocks = submission.contentBlocks.filter(block => block.type === 'TEXT' && block.content?.trim());
  const isSubmitted = submission.status === 'SUBMITTED';
  const isGraded = submission.status === 'GRADED';

  return (
    <div className={styles.page}>
      <Link className={styles.backLink} href={`/teacher/assignments/${submission.assignmentId}`}>
        Bài tập liên quan
      </Link>

      <header className={styles.header}>
        <div className={styles.headingCopy}>
          <p className={styles.eyebrow}>Không gian gia sư / Bài nộp</p>
          <div className={styles.titleLine}>
            <h1>Bài nộp</h1>
            <Tag className={`${styles.status} ${styles[`status${submission.status}`]}`}>
              {statusLabels[submission.status]}
            </Tag>
          </div>
          <p className={styles.submissionCode}>Mã bài nộp <code>{submission.id}</code></p>
        </div>
        <dl className={styles.identity}>
          <div>
            <dt>Mã học viên</dt>
            <dd><code>{submission.studentId}</code></dd>
          </div>
          <div>
            <dt>Mã bài tập</dt>
            <dd><code>{submission.assignmentId}</code></dd>
          </div>
        </dl>
      </header>

      <div className={styles.reviewLayout}>
        <section className={styles.workSection} aria-labelledby="work-heading">
          <div className={styles.sectionHeading}>
            <div>
              <p className={styles.eyebrow}>Nội dung học viên gửi</p>
              <h2 id="work-heading">Bài làm</h2>
            </div>
            <p className={styles.timestamp}>
              <span>Thời gian nộp</span>
              <time dateTime={submission.submittedAt ?? undefined}>{displayTime(submission.submittedAt)}</time>
            </p>
          </div>

          {textBlocks.length > 0 ? (
            <div className={styles.answer}>
              {textBlocks.map((block, index) => (
                <p key={`${index}-${block.content}`}>{block.content}</p>
              ))}
            </div>
          ) : (
            <p className={styles.emptyAnswer}>Bài nộp này chưa có phần nội dung văn bản.</p>
          )}
        </section>

        <section className={styles.gradeSection} aria-labelledby="grade-heading">
          <div className={styles.sectionHeading}>
            <div>
              <p className={styles.eyebrow}>Đánh giá bài làm</p>
              <h2 id="grade-heading">{isGraded ? 'Kết quả chấm' : 'Chấm bài'}</h2>
            </div>
            {isGraded && submission.gradedAt && (
              <p className={styles.timestamp}>
                <span>Đã chấm lúc</span>
                <time dateTime={submission.gradedAt}>{formatVietnamDateTime(submission.gradedAt)}</time>
              </p>
            )}
          </div>

          {isSubmitted ? (
            <Form
              key={`${submission.status}-${submission.version}`}
              className={styles.gradeForm}
              layout="vertical"
              initialValues={{ score: submission.score, feedbackText: submission.feedbackText }}
              disabled={busy}
              onFinish={saveGrade}
            >
              <Form.Item
                name="score"
                label="Điểm /10"
                rules={[{ required: true, message: 'Nhập điểm trước khi lưu.' }]}
              >
                <InputNumber
                  className={styles.scoreInput}
                  aria-label="Điểm /10"
                  min={0}
                  max={10}
                  step={0.25}
                  precision={2}
                  placeholder="0–10"
                />
              </Form.Item>
              <Form.Item name="feedbackText" label="Nhận xét">
                <Input.TextArea
                  className={styles.feedbackInput}
                  aria-label="Nhận xét"
                  rows={6}
                  placeholder="Ghi nhận xét để học viên biết điểm tốt và phần cần cải thiện."
                />
              </Form.Item>
              <Button className={styles.saveButton} type="primary" htmlType="submit" loading={busy}>
                Lưu kết quả chấm
              </Button>
            </Form>
          ) : isGraded ? (
            <dl className={styles.result}>
              <div className={styles.scoreResult}>
                <dt>Điểm</dt>
                <dd>{submission.score === null ? 'Chưa có điểm' : <><strong>{submission.score}</strong><span>/10</span></>}</dd>
              </div>
              <div className={styles.feedbackResult}>
                <dt>Nhận xét</dt>
                <dd>{submission.feedbackText?.trim() || 'Không có nhận xét.'}</dd>
              </div>
            </dl>
          ) : (
            <div className={styles.readonlyNotice}>
              <h3>Chưa thể chấm bài</h3>
              <p>Bản nháp chưa được học viên nộp nên chưa thể nhập điểm hoặc nhận xét.</p>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
