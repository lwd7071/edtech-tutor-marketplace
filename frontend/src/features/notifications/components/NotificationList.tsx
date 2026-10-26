'use client';

import { useCallback, useEffect, useState } from 'react';
import type { KeyboardEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Alert, App, Button, Pagination, Skeleton, Typography } from 'antd';
import { BookOutlined, DollarOutlined, InfoCircleOutlined, ProfileOutlined } from '@ant-design/icons';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { DateTimeText } from '@/shared/components/data-display/DateTimeText';
import { notificationApi } from '../api/notificationApi';
import type { NotificationView } from '../types';
import { studentDashboardKeys } from '@/features/student-dashboard/data/studentDashboardKeys';

type NotificationAudience = 'STUDENT' | 'TEACHER';

export const NotificationList = ({ audience }: { audience: NotificationAudience }) => {
  const router = useRouter();
  const queryClient = useQueryClient();
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
  const rolePrefix = audience === 'TEACHER' ? '/teacher/' : '/student/';
  const safeReferenceUrl = (item: NotificationView) =>
    item.referenceUrl?.startsWith(rolePrefix) && !item.referenceUrl.startsWith('//') ? item.referenceUrl : null;

  const read = async (id: string) => {
    try {
      await notificationApi.markAsRead(id);
      setItems(old => old.map(notification => notification.id === id ? { ...notification, isRead: true } : notification));
      if (audience === 'STUDENT') await queryClient.invalidateQueries({ queryKey: studentDashboardKeys.summary() });
      return true;
    } catch {
      message.error('Chưa đánh dấu được thông báo.');
      return false;
    }
  };

  const open = async (item: NotificationView) => {
    const href = safeReferenceUrl(item);
    if (!href) return;
    if (!item.isRead && !(await read(item.id))) return;
    router.push(href);
  };

  const markAll = async () => {
    try {
      await notificationApi.markAllAsRead();
      setItems(old => old.map(notification => ({ ...notification, isRead: true })));
      if (audience === 'STUDENT') await queryClient.invalidateQueries({ queryKey: studentDashboardKeys.summary() });
    } catch {
      message.error('Chưa đánh dấu được các thông báo.');
    }
  };

  const icon = (type: string | null) => ['INVOICE', 'PAYMENT', 'REFUND', 'EXTENSION'].includes(type ?? '')
    ? <DollarOutlined />
    : type === 'PROFILE' ? <ProfileOutlined />
      : ['BOOKING', 'TRIAL_REQUEST', 'ASSIGNMENT', 'SUBMISSION'].includes(type ?? '')
        ? <BookOutlined /> : <InfoCircleOutlined />;

  const tabs = [['ALL', 'Tất cả'], ['UNREAD', 'Chưa đọc'], ['BOOKING', audience === 'TEACHER' ? 'Lịch dạy' : 'Lịch học'], ['ASSIGNMENT', 'Bài tập'], ['FINANCE', audience === 'TEACHER' ? 'Thu nhập' : 'Tài chính'], ['SYSTEM', 'Hệ thống']];
  const changeTab = (key: string) => { setTab(key); setPage(0); };
  const moveTabFocus = (event: KeyboardEvent<HTMLButtonElement>, index: number) => {
    const nextIndex = event.key === 'ArrowRight' ? (index + 1) % tabs.length
      : event.key === 'ArrowLeft' ? (index - 1 + tabs.length) % tabs.length
        : event.key === 'Home' ? 0 : event.key === 'End' ? tabs.length - 1 : -1;
    if (nextIndex < 0) return;
    event.preventDefault();
    const nextKey = tabs[nextIndex][0];
    const nextButton = event.currentTarget.parentElement?.querySelector<HTMLButtonElement>(`[data-notification-tab="${nextKey}"]`);
    nextButton?.focus();
    changeTab(nextKey);
  };

  return (
    <section className="tm-notifications" aria-label="Thông báo" aria-labelledby="tm-notifications-heading">
      <header className="tm-notifications__heading">
        <div>
          <h1 id="tm-notifications-heading">Thông báo</h1>
          <p>{audience === 'TEACHER'
            ? 'Theo dõi lịch dạy, bài tập, thu nhập và trạng thái tài khoản.'
            : 'Theo dõi lịch học, bài tập, tài chính và trạng thái tài khoản.'}</p>
        </div>
        <Button className="tm-notifications__mark-all" disabled={!items.some(notification => !notification.isRead)} onClick={markAll}>
          Đánh dấu tất cả đã đọc
        </Button>
      </header>

      <div className="tm-notifications__tabs" role="tablist" aria-label="Loại thông báo">
        {tabs.map(([key, label], index) => (
          <button key={key} type="button" role="tab" data-notification-tab={key}
            id={`notification-tab-${key}`} aria-controls="notification-panel" aria-selected={tab === key}
            tabIndex={tab === key ? 0 : -1} className="tm-notifications__tab"
            onClick={() => changeTab(key)} onKeyDown={event => moveTabFocus(event, index)}>{label}</button>
        ))}
      </div>

      <div id="notification-panel" role="tabpanel" aria-labelledby={`notification-tab-${tab}`}>
        {error && (
          <Alert className="tm-notifications__error" type="error" showIcon
            title="Chưa tải được thông báo"
            action={<Button onClick={() => void fetchNotifications()}>Thử lại</Button>} />
        )}

        {loading ? (
          <div className="tm-notifications__loading" role="status" aria-label="Đang tải thông báo">
            <Skeleton active paragraph={{ rows: 4 }} />
          </div>
        ) : !error && items.length === 0 ? (
          <div className="tm-notifications__empty">
            <h2>Không có thông báo trong mục này</h2>
            <p>{audience === 'TEACHER'
              ? 'Cập nhật về lịch dạy, học viên và tài khoản sẽ xuất hiện tại đây.'
              : 'Các cập nhật về lịch học, bài tập và tài khoản sẽ xuất hiện tại đây.'}</p>
          </div>
        ) : !error && (
          <section className="tm-notifications__results" aria-label="Danh sách thông báo">
            <div className="tm-notifications__list" role="list">
              {items.map(item => {
              const href = safeReferenceUrl(item);
              const content = (
                <>
                  <span className="tm-notifications__icon" aria-hidden="true">{icon(item.referenceType)}</span>
                  <span className="tm-notifications__copy">
                    <span className="tm-notifications__title-line">
                      <Typography.Text className="tm-notifications__title" strong>{item.title}</Typography.Text>
                      <span className={item.isRead ? 'tm-notifications__read' : 'tm-notifications__unread'}>{item.isRead ? 'Đã đọc' : 'Chưa đọc'}</span>
                    </span>
                    <span className="tm-notifications__content">{item.content}</span>
                    <DateTimeText className="tm-notifications__date" value={item.createdAt} variant="relative" />
                  </span>
                </>
              );

              return (
                <article key={item.id} className={`tm-notifications__item ${item.isRead ? '' : 'is-unread'}`} role="listitem">
                  {href ? (
                    <Link className="tm-notifications__open" href={href} onClick={event => { event.preventDefault(); void open(item); }}>
                      {content}
                    </Link>
                  ) : <div className="tm-notifications__open">{content}</div>}
                  {!item.isRead && (
                    <Button className="tm-notifications__read-action" type="link" onClick={() => void read(item.id)}
                      aria-label={`Đánh dấu đã đọc: ${item.title}`}>Đánh dấu đã đọc</Button>
                  )}
                </article>
              );
              })}
            </div>
            {meta.totalPages > 1 && (
              <Pagination className="tm-notifications__pagination" current={page + 1} total={meta.totalElements}
                pageSize={meta.size} showSizeChanger={false} onChange={next => setPage(next - 1)} />
            )}
          </section>
        )}
      </div>

      <style>{`
        .tm-notifications{--notification-line:#dedbd1;--notification-ink:#171714;--notification-muted:#625f57;min-width:0;color:var(--notification-ink)}
        .tm-notifications__heading{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;margin-bottom:20px}
        .tm-notifications__heading h1{margin:0;font-size:clamp(28px,2.8vw,36px);line-height:1.15;letter-spacing:-.025em}
        .tm-notifications__heading p{margin:8px 0 0;color:var(--notification-muted);font-size:15px}
        .tm-notifications__mark-all{flex:none;min-height:42px}
        .tm-notifications__tabs{display:flex;gap:26px;max-width:100%;margin-bottom:8px;overflow-x:auto;border-bottom:1px solid var(--notification-line);scrollbar-width:thin;scrollbar-color:#aaa38f transparent}
        .tm-notifications__tab{position:relative;flex:none;min-height:44px;padding:8px 0 11px;border:0;background:transparent;color:#49473f;font:inherit;white-space:nowrap;cursor:pointer}
        .tm-notifications__tab[aria-selected="true"]{color:var(--notification-ink);font-weight:650}
        .tm-notifications__tab[aria-selected="true"]::after{position:absolute;right:0;bottom:-1px;left:0;height:2px;background:var(--notification-ink);content:""}
        .tm-notifications__tab:focus-visible{outline:3px solid #8d7000;outline-offset:-2px}
        .tm-notifications__error{margin:14px 0}
        .tm-notifications__loading{padding:12px 0}
        .tm-notifications__empty{padding:52px 20px;border-top:1px solid var(--notification-line);text-align:center}
        .tm-notifications__empty h2{margin:0;font-size:20px}.tm-notifications__empty p{margin:8px 0 0;color:var(--notification-muted)}
        .tm-notifications__list{border-top:1px solid var(--notification-line)}
        .tm-notifications__item{display:flex;align-items:center;gap:20px;padding:20px 6px;border-bottom:1px solid var(--notification-line)}
        .tm-notifications__item.is-unread{background:#fffdf5}
        .tm-notifications__open{display:flex;flex:1;min-width:0;align-items:flex-start;gap:14px;color:inherit;text-decoration:none}
        .tm-notifications__open:focus-visible{outline:3px solid #8d7000;outline-offset:3px}
        .tm-notifications__icon{display:grid;flex:none;width:34px;height:34px;place-items:center;color:#5d4d10;font-size:17px}
        .tm-notifications__copy{display:grid;flex:1;min-width:0;gap:5px}
        .tm-notifications__title-line{display:flex;min-width:0;align-items:center;gap:10px}
        .tm-notifications__title{min-width:0;color:var(--notification-ink);font-size:15px;line-height:1.4;overflow-wrap:anywhere}
        .tm-notifications__unread,.tm-notifications__read{flex:none;font-size:12px;font-weight:650}
        .tm-notifications__unread{color:#725900}.tm-notifications__read{color:var(--notification-muted)}
        .tm-notifications__content{color:#38372f;font-size:14px;line-height:1.55;overflow-wrap:anywhere}
        .tm-notifications__date{color:var(--notification-muted);font-size:12px}
        .tm-notifications__read-action{flex:none;padding-inline:4px;color:var(--notification-ink);text-align:right;white-space:normal}
        .tm-notifications__read-action:hover{color:#725900!important}
        .tm-notifications__pagination{display:flex;justify-content:flex-end;margin-top:20px}
        .tm-notifications button:focus-visible{outline:3px solid #8d7000;outline-offset:2px}
        @media(max-width:760px){.tm-notifications__heading{align-items:stretch;flex-direction:column;gap:14px}.tm-notifications__mark-all{align-self:flex-start}.tm-notifications__tabs{gap:24px;scrollbar-width:none}.tm-notifications__tabs::-webkit-scrollbar{display:none}.tm-notifications__item{align-items:stretch;flex-direction:column;gap:10px;padding:17px 4px}.tm-notifications__open{gap:10px}.tm-notifications__read-action{align-self:flex-start;margin-left:44px}.tm-notifications__pagination{justify-content:center}.tm-notifications__empty{padding:40px 14px}}
      `}</style>
    </section>
  );
};
