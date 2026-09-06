'use client';

import React, { useEffect, useState } from 'react';
import { Tabs, List, Typography, Button, Space, Badge } from 'antd';
import { BellOutlined, CheckOutlined, ProfileOutlined, BookOutlined, DollarOutlined, InfoCircleOutlined } from '@ant-design/icons';
import { useRouter } from 'next/navigation';
import { DateTimeText } from '@/shared/components/data-display/DateTimeText';
import { notificationApi } from '../api/notificationApi';
import { NotificationView } from '../types';

export const NotificationList: React.FC = () => {
  const router = useRouter();
  const [notifications, setNotifications] = useState<NotificationView[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('ALL');

  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const res = await notificationApi.getNotifications();
      setNotifications(res.data || []);
    } catch (e) {
      console.error(e);
      // Fallback for UI testing
      if ((e as any).response?.status === 404) {
        setNotifications([
          { id: '1', userId: 'u1', type: 'BOOKING', title: 'Học sinh đã đặt lịch', content: 'Học sinh Nguyễn Văn A đã đặt lịch học thử.', isRead: false, referenceId: 'b1', referenceUrl: '/teacher/bookings', createdAt: new Date().toISOString() },
          { id: '2', userId: 'u1', type: 'SYSTEM', title: 'Hồ sơ đã được duyệt', content: 'Hồ sơ giáo viên của bạn đã được quản trị viên duyệt.', isRead: true, referenceId: null, referenceUrl: null, createdAt: new Date(Date.now() - 86400000).toISOString() },
        ]);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  const handleMarkAsRead = async (e: React.MouseEvent, id: string) => {
    e.stopPropagation();
    try {
      await notificationApi.markAsRead(id);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n));
    } catch (error) {
      console.error(error);
      // Optimistic update for mock
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n));
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    } catch (error) {
      console.error(error);
      // Optimistic
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    }
  };

  const handleClick = async (notification: NotificationView) => {
    if (!notification.isRead) {
      try {
        await notificationApi.markAsRead(notification.id);
      } catch (e) { } // Ignore errors on click-through
    }
    if (notification.referenceUrl) {
      router.push(notification.referenceUrl);
    }
  };

  const filteredNotifications = notifications.filter(n => {
    if (activeTab === 'ALL') return true;
    if (activeTab === 'UNREAD') return !n.isRead;
    return n.type === activeTab;
  });

  const getIcon = (type: string) => {
    switch (type) {
      case 'BOOKING': return <BookOutlined className="text-blue-500" />;
      case 'ASSIGNMENT': return <BookOutlined className="text-orange-500" />;
      case 'FINANCE': return <DollarOutlined className="text-green-500" />;
      case 'PROFILE': return <ProfileOutlined className="text-purple-500" />;
      default: return <InfoCircleOutlined className="text-gray-500" />;
    }
  };

  return (
    <div className="bg-surface rounded-xl shadow-sm border border-border p-6 min-h-[600px]">
      <div className="flex justify-between items-center mb-6">
        <Typography.Title level={4} className="m-0">Thông báo</Typography.Title>
        <Button icon={<CheckOutlined />} onClick={handleMarkAllAsRead}>
          Đánh dấu đọc tất cả
        </Button>
      </div>

      <Tabs 
        activeKey={activeTab} 
        onChange={setActiveTab}
        items={[
          { key: 'ALL', label: 'Tất cả' },
          { key: 'UNREAD', label: 'Chưa đọc' },
          { key: 'BOOKING', label: 'Lịch học' },
          { key: 'ASSIGNMENT', label: 'Bài tập' },
          { key: 'SYSTEM', label: 'Hệ thống' },
        ]}
      />

      <List
        loading={loading}
        itemLayout="horizontal"
        dataSource={filteredNotifications}
        locale={{ emptyText: 'Bạn đã xem hết thông báo.' }}
        renderItem={item => (
          <List.Item
            onClick={() => handleClick(item)}
            className={`
              cursor-pointer p-4 rounded-lg mb-2 transition-colors border border-transparent
              ${item.isRead ? 'bg-white hover:bg-neutral-50' : 'bg-primary-50 border-primary-100 hover:bg-primary-100'}
            `}
            extra={
              !item.isRead && (
                <Button 
                  type="text" 
                  size="small" 
                  onClick={(e) => handleMarkAsRead(e, item.id)}
                >
                  Đánh dấu đã đọc
                </Button>
              )
            }
          >
            <List.Item.Meta
              avatar={
                <div className="w-10 h-10 rounded-full bg-white border border-border flex items-center justify-center text-lg">
                  {getIcon(item.type)}
                </div>
              }
              title={
                <Space>
                  <span className={`font-semibold ${item.isRead ? 'text-text-primary' : 'text-primary-700'}`}>
                    {item.title}
                  </span>
                  {!item.isRead && <Badge status="processing" />}
                </Space>
              }
              description={
                <div className="flex flex-col gap-1">
                  <span className={item.isRead ? 'text-text-secondary' : 'text-text-primary'}>
                    {item.content}
                  </span>
                  <DateTimeText value={item.createdAt} variant="relative" className="text-xs mt-1" />
                </div>
              }
            />
          </List.Item>
        )}
      />
    </div>
  );
};
