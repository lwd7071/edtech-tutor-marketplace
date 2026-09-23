'use client';

import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, App, Button, Form, Input, Skeleton } from 'antd';
import { learningApi } from '../api/learningApi';
import { learningKeys } from '../data/learningKeys';
import FileUpload from '@/shared/components/ui/FileUpload';
import { FileViewer } from '@/shared/components/data-display/FileViewer';
import { isConcurrentModification, parseApiError } from '@/shared/backend';
import { studentDashboardKeys } from '@/features/student-dashboard/data/studentDashboardKeys';
import { formatVietnamDateTime } from '@/shared/lib/vietnamTime';
import type { CreateSubmissionRequest } from '../types';

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

  if (query.isLoading) return <Skeleton active />;
  if (query.isError || !query.data?.data) return <Alert type="error" title="Chưa đọc được bài tập"
    action={<Button onClick={() => query.refetch()}>Thử lại</Button>} />;

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

  return <div className="tm-stack">
    <section className="tm-panel"><h1>{assignment.title}</h1>
      <p>Hạn nộp: {assignment.dueAt ? formatVietnamDateTime(assignment.dueAt) : 'Chưa đặt'}</p>
      {assignment.contentBlocks?.filter(block => block.type === 'TEXT').map((block, index) =>
        <p className="tm-prose" key={index}>{block.content}</p>)}
      <FileViewer files={assignment.assignmentAttachments} />
    </section>
    {overdue && <Alert type="warning" title="Đã quá hạn nộp bài" />}
    {submission && <section className="tm-panel">
      <h2>{submission.status === 'DRAFT' ? 'Bản nháp' : 'Bài đã nộp'}</h2>
      {submission.contentBlocks?.filter(block => block.type === 'TEXT').map((block, index) =>
        <p className="tm-prose" key={index}>{block.content}</p>)}
      <FileViewer files={assignment.submissionAttachments} />
      {submission.status === 'GRADED' ? <><strong>Điểm: {submission.score}/10</strong><p>{submission.feedbackText}</p></>
        : submission.status === 'SUBMITTED' ? <p>Đang chờ gia sư chấm.</p> : <p>Chưa gửi cho gia sư.</p>}
    </section>}
    {canEdit && <section className="tm-panel"><h2>{submission ? 'Cập nhật bài làm' : 'Bài làm của bạn'}</h2>
      <Form form={form} layout="vertical" initialValues={{ answer: previousAnswer }}
        onFinish={values => void save(values, 'SUBMITTED')}>
        <Form.Item name="answer" label="Câu trả lời"><Input.TextArea rows={8} /></Form.Item>
        <Form.Item label="Tệp đính kèm"><FileUpload attachableType="SUBMISSION" multiple
          onUploadSuccess={file => setAttachmentIds(ids => [...ids, file.id])} /></Form.Item>
        <div className="tm-inline">
          <Button htmlType="button" disabled={busy} onClick={() => void save(form.getFieldsValue(), 'DRAFT')}>Lưu bản nháp</Button>
          <Button htmlType="submit" type="primary" loading={busy}>Nộp bài</Button>
        </div>
      </Form>
    </section>}
  </div>;
}
