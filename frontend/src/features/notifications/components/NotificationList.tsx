'use client';

import { useCallback, useEffect, useState } from 'react';
import { Alert, App, Badge, Button, Empty, Pagination, Skeleton, Space, Tabs, Typography } from 'antd';
import { BookOutlined, CheckOutlined, DollarOutlined, InfoCircleOutlined, ProfileOutlined } from '@ant-design/icons';
import { useRouter } from 'next/navigation';
import { DateTimeText } from '@/shared/components/data-display/DateTimeText';
import { notificationApi } from '../api/notificationApi';
import type { NotificationView } from '../types';

export const NotificationList = () => {
  const router = useRouter();
  const { message } = App.useApp();
  const [items, setItems] = useState<NotificationView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [tab, setTab] = useState('ALL');
  const [page, setPage] = useState(0);
  const [meta, setMeta] = useState({ totalElements: 0, totalPages: 0, size: 20 });

  const fetchNotifications = useCallback(async () => {
    setLoading(true); setError(false);
    try {
      const unread = tab === 'UNREAD' ? false : undefined;
      const referenceType = !['ALL', 'UNREAD'].includes(tab) ? tab : undefined;
      const response = await notificationApi.getNotifications(unread, page, 20, referenceType);
      setItems(response.data ?? []);
      setMeta(response.meta ?? { totalElements: 0, totalPages: 0, size: 20 });
    } catch { setError(true); } finally { setLoading(false); }
  }, [page, tab]);

  useEffect(() => { void fetchNotifications(); }, [fetchNotifications]);
  const read = async (id: string) => { try { await notificationApi.markAsRead(id); setItems(old => old.map(n => n.id === id ? { ...n, isRead: true } : n)); return true; } catch { message.error('Chưa đánh dấu được thông báo.'); return false; } };
  const open = async (item: NotificationView) => { if (!item.isRead && !(await read(item.id))) return; if (item.referenceUrl?.startsWith('/')) router.push(item.referenceUrl); };
  const markAll = async () => { try { await notificationApi.markAllAsRead(); setItems(old => old.map(n => ({ ...n, isRead: true }))); } catch { message.error('Chưa đánh dấu được các thông báo.'); } };
  const icon = (type: string | null) => ['INVOICE', 'PAYMENT', 'REFUND', 'EXTENSION'].includes(type ?? '') ? <DollarOutlined /> : type === 'PROFILE' ? <ProfileOutlined /> : ['BOOKING', 'TRIAL_REQUEST', 'ASSIGNMENT', 'SUBMISSION'].includes(type ?? '') ? <BookOutlined /> : <InfoCircleOutlined />;

  return <div className="tm-stack">
    <header className="tm-toolbar"><div className="tm-page-heading" style={{ marginBottom: 0 }}><h1>Thông báo</h1><p>Theo dõi lịch học, bài tập, tài chính và trạng thái tài khoản.</p></div><Button icon={<CheckOutlined />} disabled={!items.some(n => !n.isRead)} onClick={markAll}>Đánh dấu tất cả đã đọc</Button></header>
    <Tabs activeKey={tab} onChange={key => { setTab(key); setPage(0); }} items={[['ALL','Tất cả'],['UNREAD','Chưa đọc'],['BOOKING','Lịch học'],['ASSIGNMENT','Bài tập'],['FINANCE','Tài chính'],['SYSTEM','Hệ thống']].map(([key,label]) => ({ key, label }))} />
    {error && <Alert type="error" showIcon title="Chưa tải được thông báo" action={<Button onClick={fetchNotifications}>Thử lại</Button>} />}
    {loading ? <Skeleton active /> : !error && <section className="tm-panel">
      {items.length === 0 ? <Empty description="Không có thông báo trong mục này." /> : <div role="list">{items.map(item => <article key={item.id} role="listitem" onClick={() => void open(item)} style={{ cursor: item.referenceUrl ? 'pointer' : 'default', background: item.isRead ? 'transparent' : 'var(--color-primary-50)', padding: 16, borderRadius: 8, marginBottom: 8, display: 'flex', gap: 12 }}>
        <div className="tm-notification-icon">{icon(item.referenceType)}</div><div style={{ flex: 1 }}><Space><Typography.Text strong>{item.title}</Typography.Text>{!item.isRead && <Badge status="processing" />}</Space><p>{item.content}</p><DateTimeText value={item.createdAt} variant="relative" /></div>
        {!item.isRead && <Button type="link" onClick={event => { event.stopPropagation(); void read(item.id); }}>Đánh dấu đã đọc</Button>}
      </article>)}</div>}
      {meta.totalPages > 1 && <Pagination current={page + 1} total={meta.totalElements} pageSize={meta.size} showSizeChanger={false} onChange={next => setPage(next - 1)} />}
    </section>}
  </div>;
};
