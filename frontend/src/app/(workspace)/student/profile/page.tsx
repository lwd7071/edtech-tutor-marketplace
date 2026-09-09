'use client';

import React, { useState } from 'react';
import { Card, Typography, Form, Input, Button, message, Avatar, Space, Divider, Switch } from 'antd';
import { UserOutlined, MailOutlined, PhoneOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useAuthStore } from '@/features/auth';
import { authApi } from '@/shared/api/auth';

export default function StudentProfilePage() {
  const { user } = useAuthStore();
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  const handleFinish = async (values: any) => {
    try {
      setSubmitting(true);
      await authApi.updateParentContact(values);
      message.success('Cập nhật thông tin phụ huynh thành công!');
    } catch (error) {
      console.error('Lỗi khi cập nhật thông tin:', error);
      message.error('Không thể cập nhật thông tin phụ huynh. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  if (!user) {
    return null;
  }

  return (
    <div style={{ maxWidth: 896, margin: '0 auto', padding: '32px 16px', display: 'flex', flexDirection: 'column', gap: 24 }}>
      <Typography.Title level={3} style={{ margin: 0 }}>Hồ sơ cá nhân</Typography.Title>
      
      <Card style={{ boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)', borderRadius: 12 }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 24 }}>
          <Avatar 
            size={100} 
            src={user.avatarUrl} 
            icon={<UserOutlined />} 
            style={{ border: '2px solid var(--color-primary-100, #CCFBF1)' }}
          />
          <div style={{ flex: 1 }}>
            <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 4 }}>{user.fullName}</Typography.Title>
            <div style={{ color: 'var(--color-text-secondary, #4B5563)', display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16 }}>
              <MailOutlined /> {user.email}
            </div>
            <Space size="large">
              <div>
                <div style={{ color: 'var(--color-text-tertiary, #9CA3AF)', fontSize: 12, textTransform: 'uppercase', fontWeight: 600, letterSpacing: '0.05em' }}>Vai trò</div>
                <div style={{ fontWeight: 500, color: 'var(--color-primary-600, #0D9488)', marginTop: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
                  <SafetyCertificateOutlined /> Học viên
                </div>
              </div>
              <div>
                <div style={{ color: 'var(--color-text-tertiary, #9CA3AF)', fontSize: 12, textTransform: 'uppercase', fontWeight: 600, letterSpacing: '0.05em' }}>Trạng thái</div>
                <div style={{ fontWeight: 500, color: 'var(--color-success-600, #16A34A)', marginTop: 4 }}>Đang hoạt động</div>
              </div>
            </Space>
          </div>
        </div>
      </Card>

      <Card style={{ boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)', borderRadius: 12 }} title="Thông tin liên hệ phụ huynh">
        <div style={{ marginBottom: 24, color: 'var(--color-text-secondary, #4B5563)' }}>
          Vui lòng cung cấp thông tin liên hệ của phụ huynh để chúng tôi có thể cập nhật kết quả học tập và thông báo quan trọng.
        </div>
        
        <Form 
          form={form} 
          layout="vertical" 
          onFinish={handleFinish}
          initialValues={{ notifyParent: true }}
          style={{ maxWidth: 576 }}
        >
          <Form.Item
            label="Họ và tên phụ huynh"
            name="parentFullName"
            rules={[
              { required: true, message: 'Vui lòng nhập họ và tên phụ huynh' },
              { min: 3, message: 'Tên quá ngắn' }
            ]}
          >
            <Input prefix={<UserOutlined className="text-text-tertiary" />} placeholder="Nhập họ và tên" size="large" />
          </Form.Item>

          <Form.Item
            label="Số điện thoại phụ huynh"
            name="parentPhone"
            rules={[
              { required: true, message: 'Vui lòng nhập số điện thoại' },
              { pattern: /^[0-9]{10,11}$/, message: 'Số điện thoại không hợp lệ' }
            ]}
          >
            <Input prefix={<PhoneOutlined className="text-text-tertiary" />} placeholder="Nhập số điện thoại" size="large" />
          </Form.Item>

          <Form.Item
            label="Email phụ huynh"
            name="parentEmail"
            rules={[
              { required: true, message: 'Vui lòng nhập email phụ huynh' },
              { type: 'email', message: 'Email không hợp lệ' }
            ]}
          >
            <Input prefix={<MailOutlined className="text-text-tertiary" />} placeholder="Nhập địa chỉ email" size="large" />
          </Form.Item>
          
          <Form.Item 
            name="notifyParent" 
            valuePropName="checked"
          >
            <Switch /> <span style={{ marginLeft: 8 }}>Đồng ý nhận thông báo qua email / SĐT phụ huynh</span>
          </Form.Item>

          <Divider />

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Button type="primary" htmlType="submit" size="large" loading={submitting}>
              Lưu thông tin
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
