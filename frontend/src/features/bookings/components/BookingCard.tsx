'use client';

import React from 'react';
import { Card, Typography, Button, Space, Tag } from 'antd';
import { VideoCameraOutlined, EnvironmentOutlined, EyeOutlined } from '@ant-design/icons';
import { BookingDetail } from '../types';
import { BookingStatusTag } from './BookingStatusTag';

interface BookingCardProps {
  booking: BookingDetail;
  onViewDetail?: (booking: BookingDetail) => void;
}

export const formatSessionTime = (startTimeStr: string, endTimeStr: string): string => {
  try {
    const start = new Date(startTimeStr);
    const end = new Date(endTimeStr);

    const startHours = start.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false });
    const endHours = end.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false });

    const dateOptions: Intl.DateTimeFormatOptions = {
      weekday: 'long',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      timeZone: 'Asia/Ho_Chi_Minh',
    };
    const dateFormatted = start.toLocaleDateString('vi-VN', dateOptions);

    return `${startHours} – ${endHours} · ${dateFormatted}`;
  } catch {
    return `${startTimeStr} – ${endTimeStr}`;
  }
};

export const BookingCard: React.FC<BookingCardProps> = ({ booking, onViewDetail }) => {
  const timeFormatted = formatSessionTime(booking.startTime, booking.endTime);

  return (
    <Card
      style={{
        borderRadius: 'var(--radius-lg, 12px)',
        border: '1px solid var(--color-border, #E7E3DC)',
        width: '100%',
      }}
      styles={{ body: { padding: 'var(--space-4, 16px)' } }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
        <div>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {booking.subject.name}
          </Typography.Title>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>
            Gia sư: <strong>{booking.teacher.fullName}</strong>
          </Typography.Text>
        </div>

        <Space size={6}>
          {booking.trial && (
            <Tag color="gold" style={{ fontWeight: 600, border: '1px solid #B45309', color: '#B45309' }}>
              Học thử
            </Tag>
          )}
          <BookingStatusTag status={booking.status} />
        </Space>
      </div>

      <div style={{ fontSize: 14, fontWeight: 500, color: 'var(--color-primary-600, #0F766E)', margin: '8px 0' }}>
        {timeFormatted}
      </div>

      <div style={{ fontSize: 13, color: 'var(--color-text-secondary, #57534E)', marginBottom: 12 }}>
        {booking.deliveryMode === 'ONLINE' ? (
          <Space>
            <VideoCameraOutlined />
            <span>Học trực tuyến</span>
          </Space>
        ) : (
          <Space>
            <EnvironmentOutlined />
            <span>Trực tiếp: {booking.locationAddress || 'Theo thỏa thuận'}</span>
          </Space>
        )}
      </div>

      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          paddingTop: 12,
          borderTop: '1px solid var(--color-border, #E7E3DC)',
        }}
      >
        <div>
          {booking.status === 'SCHEDULED' && booking.deliveryMode === 'ONLINE' && booking.meetingLink && (
            <Button
              type="primary"
              size="small"
              icon={<VideoCameraOutlined />}
              href={booking.meetingLink}
              target="_blank"
              rel="noopener noreferrer"
            >
              Vào lớp học
            </Button>
          )}
        </div>

        <Button
          size="small"
          icon={<EyeOutlined />}
          onClick={() => onViewDetail?.(booking)}
        >
          {booking.status === 'COMPLETED' ? 'Xem báo cáo buổi học' : 'Chi tiết'}
        </Button>
      </div>
    </Card>
  );
};
