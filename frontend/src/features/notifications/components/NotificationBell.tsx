'use client';

import React, { useEffect, useState } from 'react';
import { Badge, Dropdown, MenuProps, Button, Typography, Space } from 'antd';
import { BellOutlined, CheckOutlined } from '@ant-design/icons';
import { useRouter } from 'next/navigation';
import { DateTimeText } from '@/shared/components/data-display/DateTimeText';
import { notificationApi } from '../api/notificationApi';
import { NotificationView } from '../types';
import { useAuthStore } from '@/features/auth';

export const NotificationBell: React.FC = () => {
  const router = useRouter();
  const { user } = useAuthStore();
  const [notifications, setNotifications] = useState<NotificationView[]>([]);
  const [loading, setLoading] = useState(false);

  // Poll for notifications or fetch once
  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const res = await notificationApi.getNotifications(undefined, 0, 8); // top 8
      setNotifications(res.data || []);
    } catch (e) {
      // Ignore errors for bell silently, or mock
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (user) {
      fetchNotifications();
    }
  }, [user]);

  const handleMarkAllAsRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    } catch (e) {
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    }
  };

  const handleClick = async (notification: NotificationView) => {
    if (!notification.isRead) {
      try {
        await notificationApi.markAsRead(notification.id);
        setNotifications(prev => prev.map(n => n.id === notification.id ? { ...n, isRead: true } : n));
      } catch (e) {}
    }
    if (notification.referenceUrl) {
      router.push(notification.referenceUrl);
    }
  };

  const unreadCount = notifications.filter(n => !n.isRead).length;

  const items: MenuProps['items'] = [
    {
      key: 'header',
      label: (
        <div className="flex justify-between items-center py-2 border-b border-border mb-1 px-1">
          <Typography.Text strong>Thông báo</Typography.Text>
          {unreadCount > 0 && (
            <Button type="link" size="small" onClick={handleMarkAllAsRead} className="px-0">
              Đánh dấu đã đọc
            </Button>
          )}
        </div>
      ),
      disabled: true,
      className: 'cursor-default p-0 m-0',
    },
    ...notifications.map(n => ({
      key: n.id,
      label: (
        <div 
          className={`flex flex-col gap-1 w-64 md:w-80 whitespace-normal ${n.isRead ? 'opacity-70' : 'font-medium'}`}
          onClick={() => handleClick(n)}
        >
          <div className="flex justify-between items-start gap-2">
            <Typography.Text ellipsis className={n.isRead ? '' : 'text-primary'}>
              {n.title}
            </Typography.Text>
            {!n.isRead && <Badge status="processing" />}
          </div>
          <Typography.Text type="secondary" className="text-xs line-clamp-2 leading-tight">
            {n.content}
          </Typography.Text>
          <DateTimeText value={n.createdAt} variant="relative" className="text-[10px] text-text-secondary" />
        </div>
      ),
    })),
    {
      key: 'footer',
      label: (
        <div 
          className="text-center pt-2 mt-1 border-t border-border text-primary font-medium hover:underline"
          onClick={() => {
            if (user?.role === 'TEACHER') router.push('/teacher/notifications');
            else if (user?.role === 'STUDENT') router.push('/student/notifications');
          }}
        >
          Xem tất cả
        </div>
      ),
    }
  ];

  if (notifications.length === 0) {
    items.splice(1, 0, {
      key: 'empty',
      label: <div className="text-center py-4 text-text-secondary w-64">Không có thông báo nào.</div>,
      disabled: true,
    });
  }

  return (
    <Dropdown 
      menu={{ items }} 
      trigger={['click']} 
      placement="bottomRight"
      overlayClassName="rounded-xl shadow-lg border border-border"
    >
      <div className="cursor-pointer p-2 hover:bg-neutral-100 rounded-full transition-colors relative">
        <Badge count={unreadCount} size="small" offset={[2, 0]}>
          <BellOutlined className="text-xl text-text-secondary" />
        </Badge>
      </div>
    </Dropdown>
  );
};
