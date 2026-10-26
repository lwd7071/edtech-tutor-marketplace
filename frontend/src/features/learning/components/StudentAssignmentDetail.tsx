'use client';

import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, App, Form, Input, Skeleton } from 'antd';
import { learningApi } from '../api/learningApi';
import { learningKeys } from '../data/learningKeys';
import FileUpload from '@/shared/components/ui/FileUpload';
import { FileViewer } from '@/shared/components/data-display/FileViewer';
import { isConcurrentModification, parseApiError } from '@/shared/backend';
import { studentDashboardKeys } from '@/features/student-dashboard/data/studentDashboardKeys';
import { formatVietnamDateTime } from '@/shared/lib/vietnamTime';
import type { CreateSubmissionRequest } from '../types';
import { BackLink } from '@/shared/components/navigation/NavigationLinks';

export function StudentAssignmentDetail({ assignmentId }: { assignmentId: string }) {
  const [form] = Form.useForm<{ answer?: string }>();
  const query = useQuery({ queryKey: learningKeys.studentAssignment(assignmentId),
    queryFn: () => learningApi.getStudentAssignmentDetail(assignmentId) });
  const queryClient = useQueryClient();
  const [busy, setBusy] = useState(false);
  const [currentTime, setCurrentTime] = useState<number | null>(null);
  const [attachmentIds, setAttachmentIds] = useState<string[]>([]);
  const { message } = App.useApp();

  useEffect(() => {
    setCurrentTime(Date.now());
    const timer = window.setInterval(() => setCurrentTime(Date.now()), 30_000);
    return () => window.clearInterval(timer);
  }, []);

  if (query.isLoading) return <div className="tm-assignment-detail-loading" role="status" aria-label="Đang tải bài tập"><Skeleton active paragraph={{ rows: 6 }} /></div>;
  if (query.isError || !query.data?.data) return <Alert type="error" title="Chưa đọc được bài tập"
    action={<button type="button" className="tm-assignment-retry" onClick={() => query.refetch()}>Thử lại</button>} />;

  const assignment = query.data.data;
  const submission = assignment.submission;
  const previousAnswer = submission?.contentBlocks?.find(block => block.type === 'TEXT')?.content || '';
  const overdue = currentTime !== null && !!assignment.dueAt && new Date(assignment.dueAt).getTime() < currentTime;
  const canEdit = assignment.status === 'PUBLISHED' && !overdue && submission?.status !== 'GRADED';

  const save = async (values: { answer?: string }, status: CreateSubmissionRequest['status']) => {
    if (busy) return;
    setBusy(true);
    try {
      const oldFiles = submission?.contentBlocks?.filter(block => block.attachmentId).map(block => block.attachmentId!) ?? [];
      await learningApi.submitAssignment(assignmentId, {
        contentBlocks: [
          ...(values.answer?.trim() ? [{ type: 'TEXT' as const, content: values.answer.trim() }] : []),
          ...[...new Set([...oldFiles, ...attachmentIds])].map(attachmentId => ({ type: 'FILE' as const, attachmentId })),
        ],
        version: submission?.version ?? 0,
        status,
      });
      setAttachmentIds([]);
      await query.refetch();
      await queryClient.invalidateQueries({ queryKey: studentDashboardKeys.summary() });
      message.success(status === 'DRAFT' ? 'Đã lưu bản nháp' : 'Đã nộp bài');
    } catch (error) {
      if (isConcurrentModification(error)) {
        message.error('Bài làm đã được thay đổi. Vui lòng tải lại.');
        await query.refetch();
      } else message.error(parseApiError(error).message);
    } finally { setBusy(false); }
  };

  return <main className="tm-stack tm-student-assignment-detail">
    <BackLink href="/student/assignments">Bài tập của tôi</BackLink>

    <header className="tm-assignment-detail-heading">
      <h1>{assignment.title}</h1>
      <dl>
        <div><dt>Trạng thái bài</dt><dd>{assignment.status === 'CLOSED' ? 'Đã đóng' : 'Đã giao'}</dd></div>
        <div><dt>Hạn nộp</dt><dd>{assignment.dueAt ? formatVietnamDateTime(assignment.dueAt) : 'Chưa đặt'}</dd></div>
      </dl>
    </header>

    {assignment.status === 'CLOSED' && <div className="tm-assignment-closed-note" role="status">Bài tập đã đóng. Bạn vẫn có thể xem đề và bài đã lưu.</div>}
    {overdue && <Alert className="tm-assignment-overdue" type="warning" showIcon title="Đã quá hạn nộp bài" description="Bạn vẫn có thể xem đề và bài đã lưu, nhưng không thể gửi thay đổi." />}

    <section className="tm-assignment-prompt" aria-labelledby="assignment-prompt-title">
      <h2 id="assignment-prompt-title">Đề bài</h2>
      <div className="tm-assignment-prompt-copy">
        {assignment.contentBlocks?.filter(block => block.type === 'TEXT').map((block, index) => <div className="tm-prose" role="paragraph" key={index}>{block.content}</div>)}
      </div>
      <FileViewer files={assignment.assignmentAttachments} />
    </section>

    {submission && <section className="tm-assignment-submission" aria-labelledby="student-submission-title">
      <div className="tm-assignment-section-heading">
        <h2 id="student-submission-title">{submission.status === 'DRAFT' ? 'Bản nháp' : submission.status === 'GRADED' ? 'Bài đã chấm' : 'Bài đã nộp'}</h2>
        <span>{submission.status === 'DRAFT' ? 'Chưa gửi cho gia sư' : submission.status === 'GRADED' ? 'Đã có kết quả' : 'Đang chờ gia sư chấm'}</span>
      </div>
      <div className="tm-assignment-submission-copy">
        {submission.contentBlocks?.filter(block => block.type === 'TEXT').map((block, index) => <div className="tm-prose" role="paragraph" key={index}>{block.content}</div>)}
      </div>
      <FileViewer files={assignment.submissionAttachments} />
      {submission.status === 'GRADED' && <div className="tm-assignment-feedback">
        <div className="tm-assignment-score"><span>Điểm</span><output>{submission.score}/10</output></div>
        <div><h3>Nhận xét của gia sư</h3><blockquote>{submission.feedbackText}</blockquote></div>
      </div>}
      {submission.status !== 'GRADED' && <div className="tm-assignment-submission-status" role="status">{submission.status === 'SUBMITTED' ? 'Đang chờ gia sư chấm' : 'Chưa gửi cho gia sư'}</div>}
    </section>}

    {canEdit && <section className="tm-assignment-editor" aria-labelledby="assignment-editor-title">
      <header><h2 id="assignment-editor-title">{submission ? 'Cập nhật bài làm' : 'Bài làm của bạn'}</h2><div className="tm-assignment-editor-note" role="note">Chọn lưu nháp để tiếp tục sau hoặc nộp bài để gửi gia sư.</div></header>
      <Form form={form} layout="vertical" initialValues={{ answer: previousAnswer }} onFinish={values => void save(values, 'SUBMITTED')}>
        <Form.Item name="answer" label="Câu trả lời"><Input.TextArea rows={8} /></Form.Item>
        <Form.Item label="Tệp đính kèm"><FileUpload attachableType="SUBMISSION" multiple onUploadSuccess={file => setAttachmentIds(ids => [...ids, file.id])} /></Form.Item>
        <div className="tm-assignment-actions">
          <button type="button" disabled={busy} onClick={() => void save(form.getFieldsValue(), 'DRAFT')}>Lưu bản nháp</button>
          <button type="submit" disabled={busy} aria-busy={busy} className="is-primary">{busy ? 'Đang nộp bài' : 'Nộp bài'}</button>
        </div>
      </Form>
    </section>}
  </main>;
}
