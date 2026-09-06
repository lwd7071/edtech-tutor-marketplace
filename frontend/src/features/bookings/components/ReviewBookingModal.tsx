import React, { useState } from 'react';
import { Modal, Form, Input, Button, message, Rate } from 'antd';
import { bookingApi } from '@/features/bookings/api/bookingApi';

interface ReviewBookingModalProps {
  bookingId: string;
  teacherName: string;
  visible: boolean;
  onCancel: () => void;
  onSuccess: () => void;
}

export const ReviewBookingModal: React.FC<ReviewBookingModalProps> = ({
  bookingId,
  teacherName,
  visible,
  onCancel,
  onSuccess
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (values: { rating: number; comment?: string }) => {
    Modal.confirm({
      title: 'Xác nhận gửi đánh giá',
      content: 'Bạn không thể sửa đánh giá sau khi gửi. Bạn có chắc chắn muốn gửi đánh giá này không?',
      okText: 'Gửi đánh giá',
      cancelText: 'Hủy',
      onOk: async () => {
        try {
          setLoading(true);
          await bookingApi.createReview(bookingId, values);
          message.success('Cảm ơn bạn đã đánh giá buổi học!');
          form.resetFields();
          onSuccess();
        } catch (error: any) {
          message.error(error?.response?.data?.message || 'Có lỗi xảy ra khi gửi đánh giá');
        } finally {
          setLoading(false);
        }
      }
    });
  };

  return (
    <Modal
      title={`Đánh giá giáo viên ${teacherName}`}
      open={visible}
      onCancel={onCancel}
      footer={null}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        className="mt-4"
        initialValues={{ rating: 5 }}
      >
        <Form.Item
          name="rating"
          label="Chất lượng buổi học"
          rules={[{ required: true, message: 'Vui lòng chọn số sao đánh giá' }]}
        >
          <Rate className="text-2xl text-yellow-500" />
        </Form.Item>

        <Form.Item
          name="comment"
          label="Nhận xét của bạn"
          rules={[{ max: 2000, message: 'Nhận xét không được vượt quá 2000 ký tự' }]}
        >
          <Input.TextArea 
            rows={4} 
            placeholder="Chia sẻ trải nghiệm học tập của bạn..." 
            showCount
            maxLength={2000}
          />
        </Form.Item>

        <div className="flex justify-end gap-3 mt-6">
          <Button onClick={onCancel} disabled={loading}>
            Hủy
          </Button>
          <Button type="primary" htmlType="submit" loading={loading}>
            Gửi đánh giá
          </Button>
        </div>
      </Form>
    </Modal>
  );
};
