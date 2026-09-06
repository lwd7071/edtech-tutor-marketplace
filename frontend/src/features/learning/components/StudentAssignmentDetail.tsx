'use client';

import React, { useEffect, useState } from 'react';
import { Card, Typography, Spin, Space, Button, Form, Input, Divider, Alert, Modal, message } from 'antd';
import { FileOutlined, UploadOutlined, ExclamationCircleFilled } from '@ant-design/icons';
import { useRouter } from 'next/navigation';
import { DateTimeText } from '@/shared/components/data-display/DateTimeText';
import { StatusTag } from '@/shared/components/data-display/StatusTag';
import { learningApi } from '../api/learningApi';
import { TeacherAssignmentDetail } from '../types';

interface Props {
  assignmentId: string;
}

export const StudentAssignmentDetail: React.FC<Props> = ({ assignmentId }) => {
  const router = useRouter();
  const [assignment, setAssignment] = useState<TeacherAssignmentDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  const fetchDetail = async () => {
    try {
      setLoading(true);
      const res = await learningApi.getStudentAssignmentDetail(assignmentId);
      if (res.data) {
        setAssignment(res.data);
      }
    } catch (e) {
      console.error('Failed to fetch assignment detail', e);
      message.error('Không thể tải bài tập');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetail();
  }, [assignmentId]);

  const isOverdue = (dueAt: string) => {
    return new Date(dueAt).getTime() < new Date().getTime();
  };

  const handleFinish = (values: any) => {
    Modal.confirm({
      title: 'Nộp bài tập?',
      icon: <ExclamationCircleFilled />,
      content: 'Bạn có chắc chắn muốn nộp bài? Bài sau khi nộp sẽ không thể chỉnh sửa.',
      okText: 'Nộp bài',
      cancelText: 'Hủy',
      onOk: () => submitAssignment(values),
    });
  };

  const submitAssignment = async (values: any) => {
    try {
      setSubmitting(true);
      const payload: any = {
        contentBlocks: [
          {
            type: 'TEXT',
            content: values.answer,
            orderIndex: 0
          }
          // File upload integration can be added here if needed (map uploaded files to ATTACHMENT block)
        ]
      };
      await learningApi.submitAssignment(assignmentId, payload);
      message.success('Nộp bài thành công!');
      fetchDetail(); // refresh
    } catch (e) {
      console.error('Submit error:', e);
      message.error('Có lỗi xảy ra khi nộp bài');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center p-12">
        <Spin size="large" />
      </div>
    );
  }

  if (!assignment) {
    return <div>Bài tập không tồn tại</div>;
  }

  const overdue = assignment.status === 'PUBLISHED' && isOverdue(assignment.dueAt);
  const mySubmission = assignment.submissions && assignment.submissions.length > 0 ? assignment.submissions[0] : null;

  return (
    <div className="max-w-4xl mx-auto flex flex-col gap-6">
      <Card className="shadow-sm rounded-xl">
        <div className="flex justify-between items-start mb-6">
          <div>
            <Typography.Title level={3} className="m-0 mb-2">{assignment.title}</Typography.Title>
            <Space size="large" className="text-text-secondary">
              <Space>
                <span className="font-medium">Hạn nộp:</span>
                <DateTimeText value={assignment.dueAt} />
                {overdue && <span className="text-error-600 font-medium">(Quá hạn)</span>}
              </Space>
              <StatusTag domain="Assignment" status={assignment.status} />
            </Space>
          </div>
        </div>
        
        <Divider />
        
        <div className="mb-6">
          <Typography.Title level={5}>Đề bài / Hướng dẫn:</Typography.Title>
          <div className="bg-neutral-50 p-4 rounded-lg border border-border min-h-[100px] whitespace-pre-wrap">
            {assignment.contentBlocks?.find(b => b.type === 'TEXT')?.content || 'Không có hướng dẫn chi tiết.'}
          </div>
        </div>

        {/* Attachments viewer could go here */}
      </Card>

      {!mySubmission && assignment.status === 'PUBLISHED' && (
        <Card className="shadow-sm rounded-xl border-primary-200" title="Nộp bài làm">
          {overdue && (
            <Alert 
              message="Đã quá hạn nộp bài. Bạn vẫn có thể nộp nhưng sẽ bị đánh dấu là nộp muộn." 
              type="warning" 
              showIcon 
              className="mb-4"
            />
          )}
          <Form form={form} layout="vertical" onFinish={handleFinish}>
            <Form.Item 
              label="Câu trả lời của bạn" 
              name="answer" 
              rules={[{ required: true, message: 'Vui lòng nhập nội dung bài làm' }]}
            >
              <Input.TextArea rows={8} placeholder="Nhập câu trả lời chi tiết vào đây..." />
            </Form.Item>

            {/* Note: In a real app, this should integrate with the Attachment component A6.3 */}
            <Form.Item label="Đính kèm tệp">
              <div className="p-4 border border-dashed border-border rounded-lg bg-neutral-50 text-center">
                <Button icon={<UploadOutlined />}>Tải lên tệp</Button>
                <div className="text-xs text-text-tertiary mt-2">Hỗ trợ PDF, DOCX, JPG (Tối đa 10MB)</div>
              </div>
            </Form.Item>

            <div className="flex justify-end gap-3 mt-6">
              <Button onClick={() => router.back()}>Hủy</Button>
              <Button type="primary" htmlType="submit" loading={submitting}>
                Nộp bài
              </Button>
            </div>
          </Form>
        </Card>
      )}

      {mySubmission && (
        <Card className="shadow-sm rounded-xl border-success-200" title="Bài làm của bạn">
          <div className="mb-4">
            <Space>
              <span className="font-medium">Thời gian nộp:</span>
              <DateTimeText value={mySubmission.submittedAt || ''} />
              <StatusTag domain="Submission" status={mySubmission.status} />
            </Space>
          </div>
          
          <div className="bg-neutral-50 p-4 rounded-lg border border-border min-h-[100px] mb-6 whitespace-pre-wrap">
            {(mySubmission as any).contentBlocks?.find((b: any) => b.type === 'TEXT')?.content || 'Không có nội dung text.'}
          </div>

          {mySubmission.status === 'GRADED' && (
            <div className="bg-success-50 p-4 rounded-lg border border-success-200">
              <Typography.Title level={5} className="text-success-700 m-0 mb-2">Kết quả đánh giá</Typography.Title>
              <div className="flex gap-12">
                <div>
                  <span className="block text-text-secondary text-sm">Điểm số</span>
                  <span className="text-2xl font-bold text-success-600">{mySubmission.score}/10</span>
                </div>
                <div>
                  <span className="block text-text-secondary text-sm">Nhận xét của giáo viên</span>
                  <p className="mt-1 font-medium">{(mySubmission as any).feedbackText || 'Không có nhận xét.'}</p>
                </div>
              </div>
            </div>
          )}
        </Card>
      )}
    </div>
  );
};
