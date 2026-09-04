import React from 'react';
import { Drawer, Typography, Descriptions, Divider, Rate, Button } from 'antd';
import { VideoCameraOutlined, EnvironmentOutlined, MessageOutlined, BookOutlined } from '@ant-design/icons';
import { BookingDetail } from '../types';
import { BookingStatusTag } from './BookingStatusTag';
import { formatSessionTime } from './BookingCard';
import { getMeetingLink, getChatRoute, getAssignmentsRoute } from '../utils/routes';

interface BookingDetailDrawerProps {
  open: boolean;
  booking: BookingDetail | null;
  onClose: () => void;
}

export const BookingDetailDrawer: React.FC<BookingDetailDrawerProps> = ({
  open,
  booking,
  onClose,
}) => {
  if (!booking) return null;

  const teacherId = booking.teacher?.id || (booking as any).teacherId || '';
  const hasHomework = Boolean(booking.sessionReport?.followUpNote);

  return (
    <Drawer
      title="Chi tiết buổi học"
      open={open}
      onClose={onClose}
      width={480}
      destroyOnHidden
      footer={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
          <Button
            icon={<MessageOutlined />}
            href={getChatRoute(teacherId, 'STUDENT')}
          >
            Nhắn tin với gia sư
          </Button>
          {hasHomework && (
            <Button
              type="primary"
              icon={<BookOutlined />}
              href={getAssignmentsRoute({ bookingId: booking.id })}
            >
              Xem bài tập
            </Button>
          )}
        </div>
      }
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          {booking.subject.name}
        </Typography.Title>
        <BookingStatusTag status={booking.status} />
      </div>

      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label="Gia sư">{booking.teacher.fullName}</Descriptions.Item>
        <Descriptions.Item label="Học sinh">{booking.student.fullName}</Descriptions.Item>
        <Descriptions.Item label="Thời gian">
          {formatSessionTime(booking.startTime, booking.endTime)}
        </Descriptions.Item>
        <Descriptions.Item label="Hình thức">
          {booking.deliveryMode === 'ONLINE' ? 'Trực tuyến' : 'Trực tiếp'}
        </Descriptions.Item>
        {booking.deliveryMode === 'ONLINE' && booking.meetingLink && (
          <Descriptions.Item label="Phòng học">
            <Button
              type="link"
              icon={<VideoCameraOutlined />}
              href={booking.meetingLink}
              target="_blank"
              rel="noopener noreferrer"
              style={{ paddingLeft: 0 }}
            >
              Mở liên kết phòng học
            </Button>
          </Descriptions.Item>
        )}
        {booking.deliveryMode === 'OFFLINE' && booking.locationAddress && (
          <Descriptions.Item label="Địa chỉ">{booking.locationAddress}</Descriptions.Item>
        )}
      </Descriptions>

      {/* Báo cáo buổi học (SessionReport) nếu đã hoàn thành */}
      {booking.status === 'COMPLETED' && booking.sessionReport && (
        <>
          <Divider titlePlacement="left">Báo cáo Buổi học (Session Report)</Divider>
          <div style={{ backgroundColor: 'var(--color-surface-sunken, #F5F3EF)', padding: 16, borderRadius: 'var(--radius-md, 8px)' }}>
            <div style={{ marginBottom: 12 }}>
              <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                Đánh giá của giáo viên:
              </Typography.Text>
              <Rate disabled defaultValue={booking.sessionReport.teacherSelfRating} />
            </div>

            <div style={{ marginBottom: 12 }}>
              <Typography.Text strong style={{ display: 'block', fontSize: 13 }}>
                Nội dung đã học:
              </Typography.Text>
              <Typography.Paragraph style={{ margin: 0, fontSize: 14 }}>
                {booking.sessionReport.content}
              </Typography.Paragraph>
            </div>

            <div style={{ marginBottom: 12 }}>
              <Typography.Text strong style={{ display: 'block', fontSize: 13 }}>
                Nhận xét học sinh:
              </Typography.Text>
              <Typography.Paragraph style={{ margin: 0, fontSize: 14 }}>
                {booking.sessionReport.feedback}
              </Typography.Paragraph>
            </div>

            {booking.sessionReport.followUpNote && (
              <div style={{ marginBottom: 12 }}>
                <Typography.Text strong style={{ display: 'block', fontSize: 13 }}>
                  Dặn dò / Bài tập:
                </Typography.Text>
                <Typography.Paragraph style={{ margin: 0, fontSize: 14 }}>
                  {booking.sessionReport.followUpNote}
                </Typography.Paragraph>
              </div>
            )}

            {booking.sessionReport.recordLink && (
              <div>
                <Button
                  type="link"
                  href={booking.sessionReport.recordLink}
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ paddingLeft: 0 }}
                >
                  Xem video bản ghi buổi học
                </Button>
              </div>
            )}
          </div>
        </>
      )}
    </Drawer>
  );
};
