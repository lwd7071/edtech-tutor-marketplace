'use client';

import React from 'react';
import { Form, Input, Button, Card, Space } from 'antd';
import { TeacherProfile } from '@/shared/api/teacher';

interface ProfileFormProps {
  initialData: TeacherProfile;
  onSave: (data: TeacherProfile) => Promise<void>;
  onSubmitApproval: () => Promise<void>;
}

export default function ProfileForm({ initialData, onSave, onSubmitApproval }: ProfileFormProps) {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = React.useState(false);
  const isReadOnly = initialData.approvalStatus === 'PENDING_APPROVAL' || initialData.approvalStatus === 'APPROVED';

  const onFinish = async (values: TeacherProfile) => {
    setSubmitting(true);
    await onSave(values);
    setSubmitting(false);
  };

  return (
    <Card>
      <Form
        form={form}
        layout="vertical"
        initialValues={initialData}
        onFinish={onFinish}
        disabled={isReadOnly}
      >
        <Form.Item
          label="Giới thiệu bản thân"
          name="bio"
          rules={[{ required: true, message: 'Vui lòng nhập giới thiệu bản thân' }]}
        >
          <Input.TextArea rows={4} placeholder="Viết vài dòng giới thiệu về bạn..." />
        </Form.Item>

        <Form.Item
          label="Kinh nghiệm giảng dạy"
          name="experience"
          rules={[{ required: true, message: 'Vui lòng nhập kinh nghiệm giảng dạy' }]}
        >
          <Input.TextArea rows={4} placeholder="Liệt kê kinh nghiệm làm việc và giảng dạy của bạn..." />
        </Form.Item>

        <Form.Item
          label="Trình độ học vấn"
          name="education"
          rules={[{ required: true, message: 'Vui lòng nhập trình độ học vấn' }]}
        >
          <Input.TextArea rows={4} placeholder="Bằng cấp, chứng chỉ, trường học..." />
        </Form.Item>

        <Form.Item>
          <Space>
            {!isReadOnly && (
              <Button type="primary" htmlType="submit" loading={submitting}>
                Lưu thay đổi
              </Button>
            )}
            {initialData.approvalStatus === 'DRAFT' || initialData.approvalStatus === 'REJECTED' ? (
              <Button onClick={onSubmitApproval} type="default">
                Gửi hồ sơ duyệt
              </Button>
            ) : null}
          </Space>
        </Form.Item>
      </Form>
    </Card>
  );
}
