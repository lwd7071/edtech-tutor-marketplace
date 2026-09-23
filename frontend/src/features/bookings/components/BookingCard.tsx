'use client';

import React from 'react';
import { Card, Typography, Button, Space, Tag } from 'antd';
import {
  VideoCameraOutlined,
  EnvironmentOutlined,
  EyeOutlined,
  MessageOutlined,
  BookOutlined,
} from '@ant-design/icons';
import { BookingDetail } from '../types';
import { BookingStatusTag } from './BookingStatusTag';
import { getMeetingLink, getChatRoute, getAssignmentsRoute } from '../utils/routes';
import { formatVietnamSession } from '@/shared/lib/vietnamTime';

interface BookingCardProps {
  booking: BookingDetail;
  onViewDetail?: (booking: BookingDetail) => void;
  onChat?: (teacherId: string) => void;
}

export const formatSessionTime = (startTimeStr: string, endTimeStr: string): string => {
  return formatVietnamSession(startTimeStr, endTimeStr);
};

export const BookingCard: React.FC<BookingCardProps> = ({ booking, onViewDetail, onChat }) => {
  const timeFormatted = formatSessionTime(booking.startTime, booking.endTime);
  const teacherId = booking.teacher?.id || (booking as any).teacherId || '';
  const teacherName = booking.teacher?.fullName || (booking as any).teacherName || 'Gia sư';
  const subjectName = booking.subject?.name || (booking as any).subjectName || 'Môn học';
  const meetingHref = getMeetingLink(booking.meetingLink || (booking as any).meetingUrl);
  const hasHomework = Boolean(booking.sessionReport?.followUpNote);

  const handleChat = () => {
    if (onChat) {
      onChat(teacherId);
    } else {
      window.location.href = getChatRoute(teacherId, 'STUDENT');
    }
  };

  return (
    <Card
      style={{
        borderRadius: 'var(--radius-lg, 12px)',
        border: '1px solid var(--color-border)',
        width: '100%',
      }}
      styles={{ body: { padding: 'var(--space-4, 16px)' } }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
        <div>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {subjectName}
          </Typography.Title>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>
            Gia sư: <strong>{teacherName}</strong>
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

      <div style={{ fontSize: 14, fontWeight: 500, color: 'var(--color-primary-600)', margin: '8px 0' }}>
        {timeFormatted}
      </div>

      <div style={{ fontSize: 13, color: 'var(--color-text-secondary)', marginBottom: 12 }}>
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
          flexWrap: 'wrap',
          gap: 8,
          paddingTop: 12,
          borderTop: '1px solid var(--color-border)',
        }}
      >
        <Space size={8}>
          {booking.status === 'SCHEDULED' &&
            booking.deliveryMode === 'ONLINE' &&
            meetingHref && (
              <Button
                type="primary"
                size="small"
                icon={<VideoCameraOutlined />}
                href={meetingHref}
                target="_blank"
                rel="noopener noreferrer"
              >
                Vào lớp học
              </Button>
            )}

          <Button
            size="small"
            icon={<MessageOutlined />}
            onClick={handleChat}
          >
            Nhắn tin
          </Button>

          {hasHomework && (
            <Button
              size="small"
              icon={<BookOutlined />}
              href={getAssignmentsRoute({ bookingId: booking.id })}
            >
              Bài tập
            </Button>
          )}
        </Space>

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
