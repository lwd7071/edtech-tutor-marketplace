'use client';

import React, { useEffect, useState } from 'react';
import { Typography, Button, Space, Spin, Form, InputNumber, Input, message, Tag, Divider } from 'antd';
import { ArrowLeftOutlined, CheckCircleFilled } from '@ant-design/icons';
import { useRouter } from 'next/navigation';
import { FileViewer } from '@/shared/components/data-display/FileViewer';
import { learningApi } from '@/features/learning/api/learningApi';
import { SubmissionDetail } from '@/features/learning/types';

export default function GradingPage({ params }: { params: { id: string } }) {
  const router = useRouter();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [submission, setSubmission] = useState<SubmissionDetail | null>(null);

  const fetchSubmission = async () => {
    try {
      setLoading(true);
      const res = await learningApi.getSubmissionDetail(params.id);
      setSubmission(res.data);
      if (res.data) {
        form.setFieldsValue({
          score: res.data.score,
          feedbackText: res.data.feedbackText
        });
      }
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSubmission();
  }, [params.id]);

  const onFinish = async (values: any) => {
    try {
      setSubmitting(true);
      await learningApi.gradeSubmission(params.id, {
        score: values.score,
        feedbackText: values.feedbackText
      });
      message.success('Chấm điểm thành công!');
      router.back();
    } catch (error: any) {
      message.error(error?.response?.data?.message || 'Có lỗi xảy ra khi lưu điểm');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="flex justify-center items-center h-64"><Spin size="large" /></div>;
  }

  if (!submission) {
    return <div>Không tìm thấy bài nộp</div>;
  }

  // Mock student submitted files
  const mockFiles = [
    { id: '1', fileUrl: '#', originalName: 'Bai_lam_Toan.pdf', mimeType: 'application/pdf', fileSize: 512000 } as any
  ];
  
  const textBlock = submission.contentBlocks?.find(b => b.type === 'TEXT');

  return (
    <div className="max-w-[880px] mx-auto flex flex-col gap-6">
      <Space className="mb-2">
        <Button 
          type="text" 
          icon={<ArrowLeftOutlined />} 
          onClick={() => router.back()}
        />
        <Typography.Title level={4} className="m-0">Chấm bài</Typography.Title>
      </Space>

      <div className="bg-surface rounded-xl shadow-sm border border-border flex flex-col md:flex-row overflow-hidden">
        
        {/* Left Col: Submission Content */}
        <div className="flex-1 p-6 border-b md:border-b-0 md:border-r border-border">
          <div className="flex items-center justify-between mb-4">
            <Typography.Title level={5} className="m-0">Bài làm của học sinh</Typography.Title>
            {submission.status === 'GRADED' ? (
              <Tag color="success" icon={<CheckCircleFilled />}>Đã chấm</Tag>
            ) : (
              <Tag color="warning">Chờ chấm</Tag>
            )}
          </div>

          <div className="p-4 bg-neutral-50 rounded-lg border border-border mb-6">
            <p className="whitespace-pre-wrap">{textBlock?.content || 'Học sinh chỉ nộp file đính kèm, không có ghi chú.'}</p>
          </div>

          <Typography.Title level={5} className="text-sm">File bài làm</Typography.Title>
          <FileViewer files={mockFiles} />
        </div>

        {/* Right Col: Grading Form */}
        <div className="w-full md:w-80 p-6 bg-neutral-50">
          <Typography.Title level={5} className="mb-4">Đánh giá & Cho điểm</Typography.Title>
          
          <Form
            form={form}
            layout="vertical"
            onFinish={onFinish}
          >
            <Form.Item
              name="score"
              label="Điểm số (Thang 10)"
              rules={[{ required: true, message: 'Vui lòng nhập điểm' }]}
            >
              <InputNumber 
                min={0} 
                max={10} 
                step={0.5} 
                className="w-full" 
                size="large"
                placeholder="0 - 10"
              />
            </Form.Item>

            <Form.Item
              name="feedbackText"
              label="Nhận xét"
            >
              <Input.TextArea 
                rows={6} 
                placeholder="Nhập nhận xét, góp ý cho học sinh..." 
              />
            </Form.Item>

            <Divider />

            <div className="flex gap-3">
              <Button onClick={() => router.back()} className="flex-1">
                Hủy
              </Button>
              <Button type="primary" htmlType="submit" loading={submitting} className="flex-1">
                Lưu điểm
              </Button>
            </div>
          </Form>
        </div>

      </div>
    </div>
  );
}
