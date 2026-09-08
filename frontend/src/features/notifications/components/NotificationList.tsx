'use client';

import React, { useCallback, useEffect, useState } from 'react';
import { Alert, App, Badge, Button, List, Skeleton, Space, Tabs, Typography } from 'antd';
import { BookOutlined, CheckOutlined, DollarOutlined, InfoCircleOutlined, ProfileOutlined } from '@ant-design/icons';
import { useRouter } from 'next/navigation';
import { DateTimeText } from '@/shared/components/data-display/DateTimeText';
import { notificationApi } from '../api/notificationApi';
import type { NotificationView } from '../types';

export const NotificationList: React.FC = () => {
  const router = useRouter();
  const { message } = App.useApp();
  const [notifications, setNotifications] = useState<NotificationView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [activeTab, setActiveTab] = useState('ALL');

  const fetchNotifications = useCallback(async () => {
    setLoading(true);
    setError(false);
    try {
      const res = await notificationApi.getNotifications(undefined, 0, 100);
      setNotifications(res.data || []);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void fetchNotifications(); }, [fetchNotifications]);

  const markAsRead = async (id: string) => {
    try {
      await notificationApi.markAsRead(id);
      setNotifications((items) => items.map((item) => item.id === id ? { ...item, isRead: true } : item));
      return true;
    } catch {
      message.error('Chưa đánh dấu được thông báo. Vui lòng thử lại.');
      return false;
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      setNotifications((items) => items.map((item) => ({ ...item, isRead: true })));
    } catch {
      message.error('Chưa đánh dấu được các thông báo. Vui lòng thử lại.');
    }
  };

  const handleClick = async (notification: NotificationView) => {
    if (!notification.isRead && !(await markAsRead(notification.id))) return;
    if (notification.referenceUrl?.startsWith('/')) router.push(notification.referenceUrl);
  };

  const filteredNotifications = notifications.filter((item) => activeTab === 'ALL' || (activeTab === 'UNREAD' ? !item.isRead : item.type === activeTab));
  const icon = (type: string) => type === 'FINANCE' ? <DollarOutlined /> : type === 'PROFILE' ? <ProfileOutlined /> : type === 'BOOKING' || type === 'ASSIGNMENT' ? <BookOutlined /> : <InfoCircleOutlined />;

  return (
    <div className="tm-stack">
      <header className="tm-toolbar">
        <div className="tm-page-heading" style={{ marginBottom: 0 }}><h1>Thông báo</h1><p>Theo dõi lịch học, bài tập, tài chính và trạng thái tài khoản.</p></div>
        <Button icon={<CheckOutlined />} disabled={!notifications.some((item) => !item.isRead)} onClick={handleMarkAllAsRead}>Đánh dấu tất cả đã đọc</Button>
      </header>
      {error && <Alert type="error" showIcon title="Chưa tải được thông báo" action={<Button onClick={fetchNotifications}>Thử lại</Button>} />}
      {loading ? <Skeleton active /> : !error && (
        <section className="tm-panel">
          <Tabs activeKey={activeTab} onChange={setActiveTab} items={[{ key: 'ALL', label: 'Tất cả' }, { key: 'UNREAD', label: 'Chưa đọc' }, { key: 'BOOKING', label: 'Lịch học' }, { key: 'ASSIGNMENT', label: 'Bài tập' }, { key: 'SYSTEM', label: 'Hệ thống' }]} />
          <List itemLayout="horizontal" dataSource={filteredNotifications} locale={{ emptyText: 'Không có thông báo trong mục này.' }} renderItem={(item) => (
            <List.Item onClick={() => void handleClick(item)} style={{ cursor: item.referenceUrl ? 'pointer' : 'default', background: item.isRead ? 'transparent' : 'var(--color-primary-50)', paddingInline: 16, borderRadius: 8, marginBottom: 8 }} extra={!item.isRead && <Button type="link" onClick={(event) => { event.stopPropagation(); void markAsRead(item.id); }}>Đánh dấu đã đọc</Button>}>
              <List.Item.Meta avatar={<div className="tm-notification-icon">{icon(item.type)}</div>} title={<Space><Typography.Text strong>{item.title}</Typography.Text>{!item.isRead && <Badge status="processing" />}</Space>} description={<div><p style={{ margin: '0 0 4px', color: 'var(--color-text-secondary)' }}>{item.content}</p><DateTimeText value={item.createdAt} variant="relative" /></div>} />
            </List.Item>
          )} />
        </section>
      )}
    </div>
  );
};
