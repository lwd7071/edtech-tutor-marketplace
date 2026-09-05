import React from 'react';
import { Card, Tag, Button, Space, Typography, Skeleton, Avatar } from 'antd';
import {
  VideoCameraOutlined,
  MessageOutlined,
  CalendarOutlined,
  UserOutlined,
  ArrowRightOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import { BookingDetail } from '../types';
import { getUpcomingBookingFromList } from '../hooks/useUpcomingBooking';
import { getMeetingLink, getChatRoute, getBookingDetailRoute } from '../utils/routes';

const { Text, Title } = Typography;

export interface UpcomingSessionCardProps {
  booking?: BookingDetail | null;
  isLoading?: boolean;
  onViewDetail?: (booking: BookingDetail) => void;
  onChat?: (teacherId: string) => void;
  className?: string;
}

const formatSessionTime = (startTimeStr: string, endTimeStr: string) => {
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

export const UpcomingSessionCard: React.FC<UpcomingSessionCardProps> = ({
  booking,
  isLoading,
  onViewDetail,
  onChat,
  className,
}) => {
  if (isLoading) {
    return (
      <Card
        className={className}
        style={{
          borderRadius: 'var(--radius-lg, 12px)',
          border: '1px solid var(--color-border, #E7E3DC)',
          marginBottom: 16,
        }}
      >
        <Skeleton active avatar paragraph={{ rows: 2 }} />
      </Card>
    );
  }

  if (!booking) {
    return null;
  }

  const { isHappeningNow, minutesUntilStart, canJoinMeeting } = getUpcomingBookingFromList([booking]);
  const formattedTime = formatSessionTime(booking.startTime, booking.endTime);
  const subjectTitle = booking.subject?.name || (booking as any).subjectName || 'Buổi học';
  const teacherName = booking.teacher?.fullName || (booking as any).teacherName || 'Giáo viên';
  const teacherId = booking.teacher?.id || (booking as any).teacherId || '';
  const meetingHref = getMeetingLink(booking.meetingLink || (booking as any).meetingUrl);

  const handleChatClick = () => {
    if (onChat) {
      onChat(teacherId);
    } else {
      window.location.href = getChatRoute(teacherId, 'STUDENT');
    }
  };

  const handleDetailClick = () => {
    if (onViewDetail) {
      onViewDetail(booking);
    } else {
      window.location.href = getBookingDetailRoute(booking.id);
    }
  };

  return (
    <Card
      className={className}
      style={{
        borderRadius: 'var(--radius-lg, 12px)',
        border: isHappeningNow
          ? '2px solid var(--color-primary-600, #0D9488)'
          : '1px solid var(--color-border, #E7E3DC)',
        background: isHappeningNow
          ? 'linear-gradient(135deg, rgba(20,184,166,0.06) 0%, rgba(255,255,255,1) 100%)'
          : 'var(--color-surface, #FFFFFF)',
        boxShadow: 'var(--shadow-sm, 0 1px 2px 0 rgba(0, 0, 0, 0.05))',
        marginBottom: 20,
      }}
      styles={{ body: { padding: '20px 24px' } }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
        {/* Cột thông tin buổi học */}
        <div style={{ flex: 1, minWidth: 280 }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, width: '100%' }}>
            {/* Tag trạng thái */}
            <Space size={8} wrap>
              {isHappeningNow ? (
                <Tag color="success" style={{ fontWeight: 600, padding: '2px 10px', borderRadius: 6 }}>
                  ● ĐANG DIỄN RA
                </Tag>
              ) : canJoinMeeting ? (
                <Tag color="gold" style={{ fontWeight: 600, padding: '2px 10px', borderRadius: 6 }}>
                  SẮP BẮT ĐẦU ({minutesUntilStart} PHÚT NỮA)
                </Tag>
              ) : (
                <Tag color="processing" style={{ fontWeight: 500, padding: '2px 8px', borderRadius: 6 }}>
                  BUỔI HỌC KẾ TIẾP
                </Tag>
              )}

              <Tag color={booking.deliveryMode === 'ONLINE' ? 'cyan' : 'default'} style={{ borderRadius: 6 }}>
                {booking.deliveryMode === 'ONLINE' ? 'Trực tuyến' : 'Trực tiếp'}
              </Tag>
            </Space>

            {/* Môn học */}
            <Title level={4} style={{ margin: '4px 0 0 0', color: 'var(--color-text-primary, #1C1917)' }}>
              {subjectTitle}
            </Title>

            {/* Giáo viên */}
            <Space size={8} align="center">
              <Avatar size={28} icon={<UserOutlined />} style={{ backgroundColor: 'var(--color-primary-600, #0D9488)' }} />
              <Text strong style={{ color: 'var(--color-text-secondary, #44403C)' }}>
                {teacherName}
              </Text>
            </Space>

            {/* Thời gian */}
            <Space size={6} style={{ color: 'var(--color-text-tertiary, #78716C)', fontSize: 13, marginTop: 4 }}>
              <ClockCircleOutlined />
              <span>{formattedTime}</span>
            </Space>
          </div>
        </div>

        {/* Cột các nút thao tác liên mô-đun */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, justifyContent: 'center', minWidth: 160 }}>
          {booking.deliveryMode === 'ONLINE' && (
            <Button
              type="primary"
              icon={<VideoCameraOutlined />}
              disabled={!canJoinMeeting || !meetingHref}
              href={meetingHref || undefined}
              target="_blank"
              rel="noopener noreferrer"
              style={{
                height: 40,
                borderRadius: 'var(--radius-md, 8px)',
                fontWeight: 600,
                backgroundColor: canJoinMeeting ? 'var(--color-primary-600, #0D9488)' : undefined,
              }}
            >
              Vào phòng học
            </Button>
          )}

          <Space size={8}>
            <Button
              icon={<MessageOutlined />}
              onClick={handleChatClick}
              style={{
                height: 36,
                borderRadius: 'var(--radius-md, 8px)',
                flex: 1,
              }}
            >
              Nhắn tin
            </Button>

            <Button
              type="link"
              onClick={handleDetailClick}
              style={{
                height: 36,
                padding: '4px 8px',
                color: 'var(--color-primary-700, #0F766E)',
                fontWeight: 500,
              }}
            >
              Xem chi tiết <ArrowRightOutlined />
            </Button>
          </Space>
        </div>
      </div>
    </Card>
  );
};
