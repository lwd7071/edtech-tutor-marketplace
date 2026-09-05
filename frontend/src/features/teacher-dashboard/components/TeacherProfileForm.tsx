'use client';

import React, { useEffect, useState } from 'react';
import { Form, Input, Button, message, Skeleton } from 'antd';
import { teacherApi, TeacherProfile } from '@/shared/api/teacher';

const { TextArea } = Input;

export const TeacherProfileForm: React.FC = () => {
  const [form] = Form.useForm<TeacherProfile>();
  const [profile, setProfile] = useState<TeacherProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const data = await teacherApi.getProfile();
        setProfile(data);
      } catch (error) {
        message.error('Không thể tải hồ sơ giáo viên');
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, []);

  const onFinish = async (values: TeacherProfile) => {
    setSubmitting(true);
    try {
      await teacherApi.updateProfile(values);
      message.success('Cập nhật hồ sơ thành công');
    } catch (error) {
      message.error('Cập nhật hồ sơ thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div data-testid="loading-skeleton"><Skeleton active /></div>;
  }

  return (
    <div className="bg-surface p-6 rounded-xl border border-border shadow-sm max-w-3xl">
      <h2 className="text-xl font-bold mb-6 text-text-primary">Hồ sơ Giảng dạy</h2>
      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        initialValues={profile || {}}
        requiredMark={false}
      >
        <Form.Item
          name="bio"
          label="Tiểu sử (Bio)"
          rules={[{ required: true, message: 'Vui lòng nhập tiểu sử' }]}
        >
          <TextArea 
            rows={4} 
            placeholder="Giới thiệu về bản thân và phong cách giảng dạy của bạn..." 
            className="w-full"
          />
        </Form.Item>

        <Form.Item
          name="experience"
          label="Kinh nghiệm làm việc"
          rules={[{ required: true, message: 'Vui lòng nhập kinh nghiệm' }]}
        >
          <TextArea 
            rows={4} 
            placeholder="Liệt kê kinh nghiệm làm việc, giảng dạy của bạn..." 
          />
        </Form.Item>

        <Form.Item
          name="education"
          label="Học vấn / Bằng cấp"
          rules={[{ required: true, message: 'Vui lòng nhập học vấn' }]}
        >
          <TextArea 
            rows={4} 
            placeholder="Liệt kê các trường đã học, bằng cấp, chứng chỉ liên quan..." 
          />
        </Form.Item>

        <Form.Item className="mb-0 mt-6 flex justify-end">
          <Button type="primary" htmlType="submit" loading={submitting}>
            Lưu thay đổi
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
};
