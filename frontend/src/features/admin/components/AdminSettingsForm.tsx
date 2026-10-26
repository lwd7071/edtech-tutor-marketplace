'use client';

import React, { useEffect } from 'react';
import { Button, Form, InputNumber, Skeleton, Typography } from 'antd';
import { SaveOutlined } from '@ant-design/icons';
import type { PlatformSettingsView, UpdatePlatformSettingsRequest } from '../types';
import { formatLedgerTime } from '@/features/finance';
import styles from './AdminSettingsForm.module.css';

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

export const AdminSettingsForm: React.FC<AdminSettingsFormProps> = ({ settings, loading = false, submitting = false, onUpdate }) => {
  const [form] = Form.useForm<FormValues>();

  useEffect(() => {
    if (!settings) return;
    form.setFieldsValue({
      commissionPercent: Number((settings.commissionRate * 100).toFixed(2)),
      bayesianMinimumReviews: settings.bayesianMinimumReviews,
      bookingReminderHours: settings.bookingReminderHours,
      bookingExpirationHours: settings.bookingExpirationHours,
    });
  }, [settings, form]);

  const handleFinish = async (values: FormValues) => {
    await onUpdate({
      commissionRate: values.commissionPercent / 100,
      bayesianMinimumReviews: values.bayesianMinimumReviews,
      bookingReminderHours: values.bookingReminderHours,
      bookingExpirationHours: values.bookingExpirationHours,
    });
  };

  if (loading) return <div className={styles.loading} aria-label="Đang tải cài đặt nền tảng"><Skeleton active title paragraph={{ rows: 6 }} /></div>;

  return <section className={styles.settings} aria-labelledby="platform-settings-title">
    <div className={styles.sectionHeading}>
      <div>
        <h2 id="platform-settings-title">Tham số nền tảng</h2>
        <p>Thay đổi có hiệu lực với giao dịch và lịch học phát sinh sau khi lưu.</p>
      </div>
      {settings?.updatedAt && <Typography.Text className={styles.updated}>Cập nhật lần cuối · {formatLedgerTime(settings.updatedAt)}</Typography.Text>}
    </div>

    <div className={styles.notice} role="note">
      Tỉ lệ hoa hồng được áp dụng khi hoàn thành buổi học. Các mốc thời gian điều khiển nhắc lịch và hết hạn xác nhận.
    </div>

    <Form form={form} layout="vertical" onFinish={handleFinish} className={styles.form}>
      <Form.Item
        className={styles.field}
        label="Tỉ lệ hoa hồng sàn (%)"
        name="commissionPercent"
        extra="Tỉ lệ khấu trừ trên mỗi buổi học hoàn thành thành công. Mặc định hiện tại: 5%."
        rules={[{ required: true, message: 'Vui lòng nhập tỉ lệ hoa hồng' }, { type: 'number', min: 0, max: 100, message: 'Tỉ lệ từ 0% đến 100%' }]}
      ><InputNumber className={styles.number} min={0} max={100} step={0.5} suffix="%" /></Form.Item>

      <Form.Item
        className={styles.field}
        label="Số đánh giá tối thiểu tính Bayesian Average"
        name="bayesianMinimumReviews"
        extra="Số lượt đánh giá tối thiểu để áp dụng thuật toán xếp hạng uy tín gia sư."
        rules={[{ required: true, message: 'Vui lòng nhập số đánh giá tối thiểu' }]}
      ><InputNumber className={styles.number} min={1} max={100} suffix="đánh giá" /></Form.Item>

      <Form.Item
        className={styles.field}
        label="Thời gian gửi nhắc lịch học trước giờ bắt đầu"
        name="bookingReminderHours"
        extra="Số giờ trước buổi học để hệ thống gửi thông báo và email nhắc lịch."
        rules={[{ required: true, message: 'Vui lòng nhập số giờ nhắc nhở' }]}
      ><InputNumber className={styles.number} min={1} max={72} suffix="giờ" /></Form.Item>

      <Form.Item
        className={styles.field}
        label="Thời gian tối đa để gia sư xác nhận hoàn thành buổi học"
        name="bookingExpirationHours"
        extra="Sau khoảng thời gian này kể từ end_time, nếu không có báo cáo, buổi học tự động chuyển sang EXPIRED."
        rules={[{ required: true, message: 'Vui lòng nhập số giờ hết hạn' }]}
      ><InputNumber className={styles.number} min={1} max={72} suffix="giờ" /></Form.Item>

      <Form.Item className={styles.submit}>
        <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={submitting}>Lưu cấu hình</Button>
      </Form.Item>
    </Form>
  </section>;
};
