'use client';

import React, { useState } from 'react';
import { Form, Input, Button, DatePicker, Select, Typography, message, Space } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useRouter } from 'next/navigation';
import FileUpload from '@/shared/components/ui/FileUpload';
import { FileViewer } from '@/shared/components/data-display/FileViewer';
import { learningApi } from '@/features/learning/api/learningApi';
import { AttachmentView } from '@/shared/api/attachmentApi';

export default function CreateAssignmentPage() {
  const router = useRouter();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [attachments, setAttachments] = useState<AttachmentView[]>([]);

  // Mock data for student selection until we have a real API
  const studentOptions = [
    { label: 'Nguyễn Văn A (Toán lớp 10)', value: 'student-id-1' },
    { label: 'Trần Thị B (Lý lớp 11)', value: 'student-id-2' },
  ];

  const handleUploadSuccess = (attachment: AttachmentView) => {
    setAttachments(prev => [...prev, attachment]);
  };

  const onFinish = async (values: any) => {
    try {
      setLoading(true);
      
      const contentBlocks = attachments.map(att => ({
        type: (att.mimeType.includes('image') ? 'IMAGE' : 'FILE') as 'IMAGE' | 'FILE',
        attachmentId: att.id
      }));

      // Add text instruction as the first block if provided
      if (values.instructions) {
        contentBlocks.unshift({
          type: 'TEXT',
          content: values.instructions,
          attachmentId: undefined
        } as any);
      }

      await learningApi.createAssignment({
        title: values.title,
        studentId: values.studentId,
        subjectId: 'mock-subject-id', // In reality, depends on the selected student/package
        assignmentType: 'HOMEWORK',
        dueAt: values.dueAt.toISOString(),
        status: 'PUBLISHED',
        contentBlocks,
      });

      message.success('Giao bài tập thành công');
      router.push('/teacher/assignments');
    } catch (error: any) {
      console.error(error);
      message.error(error?.response?.data?.message || 'Có lỗi xảy ra khi tạo bài tập');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-[880px] mx-auto bg-surface rounded-xl p-6 shadow-sm border border-border">
      <Space className="mb-6">
        <Button 
          type="text" 
          icon={<ArrowLeftOutlined />} 
          onClick={() => router.back()}
        />
        <Typography.Title level={4} className="m-0">Tạo bài tập mới</Typography.Title>
      </Space>

      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
      >
        <Form.Item
          name="title"
          label="Tiêu đề bài tập"
          rules={[{ required: true, message: 'Vui lòng nhập tiêu đề' }]}
        >
          <Input placeholder="VD: Bài tập về nhà tuần 1 - Đại số" size="large" />
        </Form.Item>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Form.Item
            name="studentId"
            label="Giao cho học sinh"
            rules={[{ required: true, message: 'Vui lòng chọn học sinh' }]}
          >
            <Select 
              options={studentOptions} 
              placeholder="Chọn học sinh" 
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="dueAt"
            label="Hạn nộp"
            rules={[{ required: true, message: 'Vui lòng chọn hạn nộp' }]}
          >
            <DatePicker 
              showTime 
              format="DD/MM/YYYY HH:mm" 
              className="w-full" 
              size="large"
              placeholder="Chọn thời gian"
            />
          </Form.Item>
        </div>

        <Form.Item
          name="instructions"
          label="Hướng dẫn / Đề bài"
        >
          <Input.TextArea 
            rows={6} 
            placeholder="Nhập nội dung đề bài hoặc hướng dẫn làm bài..." 
          />
        </Form.Item>

        <Form.Item label="File đính kèm (Đề thi PDF, Word, Ảnh...)">
          <FileUpload 
            attachableType="ASSIGNMENT"
            onUploadSuccess={handleUploadSuccess}
          />
          {attachments.length > 0 && (
            <div className="mt-4">
              <Typography.Text strong>File đã tải lên:</Typography.Text>
              <FileViewer files={attachments} />
            </div>
          )}
        </Form.Item>

        <div className="flex justify-end gap-4 mt-8 pt-6 border-t border-border">
          <Button onClick={() => router.back()} size="large">
            Hủy
          </Button>
          <Button type="primary" htmlType="submit" loading={loading} size="large">
            Giao bài tập
          </Button>
        </div>
      </Form>
    </div>
  );
}
