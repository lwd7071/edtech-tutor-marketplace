'use client';

import React, { useState } from 'react';
import { Modal, Form, Radio, Input, Button, message, Typography, Space } from 'antd';
import { DeliveryMode, CreateBookingRequest } from '../types';
import { useCreateBooking } from '../hooks/useBookings';

interface CreateBookingModalProps {
  open: boolean;
  studentPackageId: string;
  onClose: () => void;
}

export const CreateBookingModal: React.FC<CreateBookingModalProps> = ({
  open,
  studentPackageId,
  onClose,
}) => {
  const [form] = Form.useForm<CreateBookingRequest>();
  const [deliveryMode, setDeliveryMode] = useState<DeliveryMode>('ONLINE');
  const createMutation = useCreateBooking();

  const handleSubmit = async (values: CreateBookingRequest) => {
    try {
      await createMutation.mutateAsync({
        ...values,
        startTime: new Date(values.startTime).toISOString(),
        endTime: new Date(values.endTime).toISOString(),
        studentPackageId,
        deliveryMode,
      });
      message.success('Tạo lịch học mới thành công!');
      form.resetFields();
      onClose();
    } catch (err: any) {
      const errorCode = err?.response?.data?.errors?.[0]?.code;
      if (errorCode === 'BOOKING_TIME_CONFLICT') {
        message.error('Trùng lịch! Giáo viên hoặc học sinh đã có lịch học trong khung giờ này.');
      } else {
        message.error('Không thể đặt lịch học. Vui lòng kiểm tra lại thông tin.');
      }
    }
  };

  return (
    <Modal
      title="Đặt lịch buổi học mới"
      open={open}
      onCancel={() => {
        form.resetFields();
        onClose();
      }}
      footer={null}
      destroyOnHidden
      width={520}
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{ deliveryMode: 'ONLINE' }}
      >
        <Form.Item
          name="startTime"
          label="Thời gian bắt đầu (giờ địa phương)"
          rules={[{ required: true, message: 'Vui lòng nhập thời gian bắt đầu' }]}
        >
          <Input type="datetime-local" />
        </Form.Item>

        <Form.Item
          name="endTime"
          label="Thời gian kết thúc (giờ địa phương)"
          rules={[{ required: true, message: 'Vui lòng nhập thời gian kết thúc' }]}
        >
          <Input type="datetime-local" />
        </Form.Item>

        <Form.Item label="Hình thức học" required>
          <Radio.Group
            value={deliveryMode}
            onChange={(e) => setDeliveryMode(e.target.value)}
          >
            <Radio value="ONLINE">Trực tuyến (ONLINE)</Radio>
            <Radio value="OFFLINE">Trực tiếp (OFFLINE)</Radio>
          </Radio.Group>
        </Form.Item>

        {deliveryMode === 'ONLINE' ? (
          <Form.Item
            name="meetingLink"
            label="Liên kết phòng học (Google Meet / Zoom)"
            rules={[{ required: true, message: 'Vui lòng nhập liên kết phòng học' }]}
          >
            <Input placeholder="https://meet.google.com/..." />
          </Form.Item>
        ) : (
          <Form.Item
            name="locationAddress"
            label="Địa chỉ học trực tiếp"
            rules={[{ required: true, message: 'Vui lòng nhập địa chỉ học' }]}
          >
            <Input placeholder="Số nhà, đường, phường/xã..." />
          </Form.Item>
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
          <Button onClick={onClose}>Hủy</Button>
          <Button type="primary" htmlType="submit" loading={createMutation.isPending}>
            Xác nhận tạo lịch học
          </Button>
        </div>
      </Form>
    </Modal>
  );
};
