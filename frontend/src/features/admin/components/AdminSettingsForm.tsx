'use client';

import React, { useEffect } from 'react';
import { Card, Form, InputNumber, Button, Typography, Space, Alert } from 'antd';
import { SettingOutlined, SaveOutlined } from '@ant-design/icons';
import { PlatformSettingsView, UpdatePlatformSettingsRequest } from '../types';
import { formatLedgerTime } from '@/features/finance';

interface AdminSettingsFormProps {
  settings?: PlatformSettingsView | null;
  loading?: boolean;
  submitting?: boolean;
  onUpdate: (data: UpdatePlatformSettingsRequest) => Promise<void> | void;
}

interface FormValues {
  commissionPercent: number;
  bayesianMinimumReviews: number;
  bookingReminderHours: number;
  bookingExpirationHours: number;
}

export const AdminSettingsForm: React.FC<AdminSettingsFormProps> = ({
  settings,
  loading = false,
  submitting = false,
  onUpdate,
}) => {
  const [form] = Form.useForm<FormValues>();

  useEffect(() => {
    if (settings) {
      form.setFieldsValue({
        commissionPercent: Number((settings.commissionRate * 100).toFixed(2)),
        bayesianMinimumReviews: settings.bayesianMinimumReviews,
        bookingReminderHours: settings.bookingReminderHours,
        bookingExpirationHours: settings.bookingExpirationHours,
      });
    }
  }, [settings, form]);

  const handleFinish = async (values: FormValues) => {
    await onUpdate({
      commissionRate: values.commissionPercent / 100,
      bayesianMinimumReviews: values.bayesianMinimumReviews,
      bookingReminderHours: values.bookingReminderHours,
      bookingExpirationHours: values.bookingExpirationHours,
    });
  };

  return (
    <Card
      loading={loading}
      title={
        <Space>
          <SettingOutlined style={{ color: 'var(--color-primary-600, #0F766E)' }} />
          <span>Cấu hình Tham số Nền tảng (Platform Settings)</span>
        </Space>
      }
      extra={
        settings?.updatedAt && (
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            Cập nhật lần cuối: {formatLedgerTime(settings.updatedAt)}
          </Typography.Text>
        )
      }
      style={{
        maxWidth: 720,
        borderRadius: 'var(--radius-lg, 12px)',
        background: 'var(--color-surface, #FFFFFF)',
      }}
    >
      <Alert
        type="info"
        showIcon
        title="Lưu ý cấu hình hệ thống"
        description="Các thay đổi về tỉ lệ hoa hồng và thời gian tự động xử lý sẽ có hiệu lực tức thì đối với các giao dịch và lịch học phát sinh sau thời điểm cập nhật."
        style={{ marginBottom: 20 }}
      />

      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          label="Tỉ lệ hoa hồng sàn (%)"
          name="commissionPercent"
          tooltip="Tỉ lệ hoa hồng khấu trừ trên mỗi buổi học hoàn thành thành công (Mặc định: 5%)"
          rules={[
            { required: true, message: 'Vui lòng nhập tỉ lệ hoa hồng' },
            { type: 'number', min: 0, max: 100, message: 'Tỉ lệ từ 0% đến 100%' },
          ]}
        >
          <InputNumber style={{ width: '100%' }} min={0} max={100} step={0.5} suffix="%" />
        </Form.Item>

        <Form.Item
          label="Số đánh giá tối thiểu tính Bayesian Average"
          name="bayesianMinimumReviews"
          tooltip="Số lượt đánh giá tối thiểu để áp dụng thuật toán xếp hạng uy tín gia sư"
          rules={[{ required: true, message: 'Vui lòng nhập số đánh giá tối thiểu' }]}
        >
          <InputNumber style={{ width: '100%' }} min={1} max={100} suffix="đánh giá" />
        </Form.Item>

        <Form.Item
          label="Thời gian gửi nhắc lịch học trước giờ bắt đầu"
          name="bookingReminderHours"
          tooltip="Số giờ trước khi buổi học diễn ra để hệ thống tự động gửi thông báo và email nhắc nhở"
          rules={[{ required: true, message: 'Vui lòng nhập số giờ nhắc nhở' }]}
        >
          <InputNumber style={{ width: '100%' }} min={1} max={72} suffix="giờ" />
        </Form.Item>

        <Form.Item
          label="Thời gian tối đa để gia sư xác nhận hoàn thành buổi học"
          name="bookingExpirationHours"
          tooltip="Sau khoảng thời gian này kể từ end_time của buổi học nếu không có báo cáo, buổi học sẽ tự động chuyển sang EXPIRED"
          rules={[{ required: true, message: 'Vui lòng nhập số giờ hết hạn' }]}
        >
          <InputNumber style={{ width: '100%' }} min={1} max={72} suffix="giờ" />
        </Form.Item>

        <Form.Item style={{ marginTop: 24, marginBottom: 0 }}>
          <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={submitting}>
            Lưu cấu hình
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
};
