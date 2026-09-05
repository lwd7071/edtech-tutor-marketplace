import React from 'react';
import { Dropdown, Button } from 'antd';
import { BellOutlined } from '@ant-design/icons';
import { Badge } from '../data-display/Badge';
import NotificationItem, { NotificationItemProps } from './NotificationItem';
import Link from 'next/link';

interface NotificationBellProps {
  unreadCount: number;
  notifications: NotificationItemProps[];
  onViewAll?: () => void;
  viewAllLink?: string;
}

export default function NotificationBell({
  unreadCount,
  notifications,
  onViewAll,
  viewAllLink = '/notifications',
}: NotificationBellProps) {
  
  const dropdownContent = (
    <div className="noti-dropdown-menu">
      <div className="noti-dropdown-header">
        <h3 className="noti-dropdown-title">Thông báo</h3>
        {unreadCount > 0 && <span className="noti-dropdown-meta">{unreadCount} chưa đọc</span>}
      </div>
      
      <div className="noti-dropdown-list">
        {notifications.length > 0 ? (
          notifications.map(noti => (
            <NotificationItem key={noti.id} {...noti} />
          ))
        ) : (
          <div className="noti-empty">Bạn không có thông báo nào.</div>
        )}
      </div>
      
      <div className="noti-dropdown-footer">
        <Link 
          href={viewAllLink} 
          className="view-all-link"
          onClick={(e) => {
            if (onViewAll) {
              e.preventDefault();
              onViewAll();
            }
          }}
        >
          Xem tất cả
        </Link>
      </div>

      <style>{`
        .noti-dropdown-menu {
          width: 360px;
          background-color: var(--color-surface);
          border-radius: var(--radius-lg);
          box-shadow: var(--shadow-lg);
          border: var(--border-default);
          overflow: hidden;
          display: flex;
          flex-direction: column;
          max-height: 480px;
        }
        @media (max-width: 576px) {
          .noti-dropdown-menu {
            width: 320px;
          }
        }
        .noti-dropdown-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: var(--space-3) var(--space-4);
          border-bottom: var(--border-default);
          background-color: var(--color-surface-sunken);
        }
        .noti-dropdown-title {
          margin: 0;
          font-size: var(--text-h4);
          font-weight: var(--weight-bold);
          color: var(--color-text-primary);
        }
        .noti-dropdown-meta {
          font-size: var(--text-caption);
          color: var(--color-primary-600);
          background-color: var(--color-primary-50);
          padding: 2px 8px;
          border-radius: var(--radius-full);
          font-weight: var(--weight-medium);
        }
        .noti-dropdown-list {
          overflow-y: auto;
          flex: 1;
        }
        .noti-empty {
          padding: var(--space-6) var(--space-4);
          text-align: center;
          color: var(--color-text-tertiary);
          font-size: var(--text-body-sm);
        }
        .noti-dropdown-footer {
          border-top: var(--border-default);
          padding: var(--space-3);
          text-align: center;
        }
        .view-all-link {
          display: block;
          width: 100%;
          color: var(--color-primary-600);
          font-weight: var(--weight-medium);
          text-decoration: none;
        }
        .view-all-link:hover {
          color: var(--color-primary-700);
          text-decoration: underline;
        }
      `}</style>
    </div>
  );

  return (
    <Dropdown 
      dropdownRender={() => dropdownContent} 
      trigger={['click']} 
      placement="bottomRight"
    >
      <Badge count={unreadCount} overflowCount={99}>
        <Button 
          type="text" 
          icon={<BellOutlined style={{ fontSize: 20 }} />} 
          style={{ width: 40, height: 40, borderRadius: 'var(--radius-full)' }} 
        />
      </Badge>
    </Dropdown>
  );
}
