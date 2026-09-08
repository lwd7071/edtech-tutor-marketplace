'use client';

import React from 'react';
import { Modal, Form, Input, Rate, Button, message, Typography } from 'antd';
import { BookingDetail, SessionReport } from '../types';
import { useCompleteBooking } from '../hooks/useBookings';

interface SessionReportModalProps {
  open: boolean;
  booking: BookingDetail | null;
  onClose: () => void;
}

export const SessionReportModal: React.FC<SessionReportModalProps> = ({
  open,
  booking,
  onClose,
}) => {
  const [form] = Form.useForm<SessionReport>();
  const completeMutation = useCompleteBooking();

  if (!booking) return null;

  const handleSubmit = async (values: SessionReport) => {
    try {
      await completeMutation.mutateAsync({
        id: booking.id,
        data: {
          version: booking.version,
          report: {
            ...values,
            teacherSelfRating: values.teacherSelfRating || 5,
          },
        },
      });
      message.success('Đã nộp báo cáo và hoàn thành buổi học!');
      form.resetFields();
      onClose();
    } catch {
      message.error('Hoàn thành buổi học thất bại. Vui lòng kiểm tra lại.');
    }
  };

  return (
    <Modal
      title="Báo cáo Hoàn thành Buổi học"
      open={open}
      onCancel={() => {
        form.resetFields();
        onClose();
      }}
      footer={null}
      destroyOnHidden
      width={560}
    >
      <Typography.Paragraph type="secondary">
        Buổi học môn <strong>{booking.subject.name}</strong> với học viên <strong>{booking.student.fullName}</strong>.
      </Typography.Paragraph>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{ teacherSelfRating: 5 }}
      >
        <Form.Item
          name="content"
          label="Nội dung bài dạy"
          rules={[{ required: true, message: 'Vui lòng nhập nội dung đã dạy' }]}
        >
          <Input.TextArea rows={3} placeholder="Nhập tóm tắt nội dung đã dạy trong buổi học..." />
        </Form.Item>

        <Form.Item
          name="feedback"
          label="Nhận xét học viên"
          rules={[{ required: true, message: 'Vui lòng nhập nhận xét học viên' }]}
        >
          <Input.TextArea rows={3} placeholder="Nhận xét tinh thần và mức độ tiếp thu của học viên..." />
        </Form.Item>

        <Form.Item name="followUpNote" label="Dặn dò / Bài tập về nhà">
          <Input.TextArea rows={2} placeholder="Bài tập cần làm hoặc nội dung cần chuẩn bị buổi tới..." />
        </Form.Item>

        <Form.Item name="recordLink" label="Liên kết bản ghi video (nếu có)">
          <Input placeholder="https://drive.google.com/..." />
        </Form.Item>

        <Form.Item
          name="teacherSelfRating"
          label="Đánh giá hiệu quả buổi dạy"
          rules={[{ required: true, message: 'Vui lòng chọn mức đánh giá' }]}
        >
          <Rate />
        </Form.Item>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
          <Button onClick={onClose}>Hủy</Button>
          <Button type="primary" htmlType="submit" loading={completeMutation.isPending}>
            Hoàn thành & Gửi báo cáo
          </Button>
        </div>
      </Form>
    </Modal>
  );
};
