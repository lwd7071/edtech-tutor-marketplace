import React from 'react';
import { BellOutlined } from '@ant-design/icons';
import { DateTimeText } from '../data-display/DateTimeText';

export interface NotificationItemProps {
  id: string;
  title: string;
  message: string;
  timestamp: string;
  isRead: boolean;
  icon?: React.ReactNode;
  onClick?: () => void;
  className?: string;
}

export default function NotificationItem({
  title,
  message,
  timestamp,
  isRead,
  icon = <BellOutlined />,
  onClick,
  className = '',
}: NotificationItemProps) {
  return (
    <div 
      className={`notification-item ${isRead ? 'read' : 'unread'} ${onClick ? 'clickable' : ''} ${className}`}
      onClick={onClick}
      role={onClick ? 'button' : 'listitem'}
      tabIndex={onClick ? 0 : undefined}
    >
      <div className="noti-icon-wrapper">
        {icon}
        {!isRead && <span className="unread-dot" />}
      </div>
      <div className="noti-content">
        <h4 className="noti-title">{title}</h4>
        <p className="noti-message">{message}</p>
        <div className="noti-time">
          <DateTimeText value={timestamp} variant="relative" style={{ fontSize: '12px' }} />
        </div>
      </div>

      <style>{`
        .notification-item {
          display: flex;
          gap: var(--space-3);
          padding: var(--space-3) var(--space-4);
          border-bottom: var(--border-default);
          background-color: var(--color-surface);
          transition: background-color var(--duration-fast) var(--ease-standard);
        }
        .notification-item:last-child {
          border-bottom: none;
        }
        .notification-item.unread {
          background-color: var(--color-primary-50);
        }
        .notification-item.clickable {
          cursor: pointer;
        }
        .notification-item.clickable:hover {
          background-color: var(--color-surface-hover);
        }
        .notification-item.unread.clickable:hover {
          background-color: var(--color-primary-100);
        }
        
        .noti-icon-wrapper {
          position: relative;
          width: 32px;
          height: 32px;
          flex-shrink: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          background-color: var(--color-surface-sunken);
          border-radius: var(--radius-full);
          color: var(--color-text-secondary);
          font-size: 16px;
        }
        .notification-item.unread .noti-icon-wrapper {
          color: var(--color-primary-600);
          background-color: #E0F2FE; /* slight accent */
        }
        .unread-dot {
          position: absolute;
          top: -2px;
          right: -2px;
          width: 10px;
          height: 10px;
          background-color: var(--color-error-600);
          border: 2px solid var(--color-surface);
          border-radius: var(--radius-full);
        }
        
        .noti-content {
          flex: 1;
          min-width: 0;
        }
        .noti-title {
          margin: 0 0 2px 0;
          font-size: var(--text-body-sm);
          font-weight: var(--weight-semibold);
          color: var(--color-text-primary);
        }
        .noti-message {
          margin: 0 0 var(--space-1) 0;
          font-size: var(--text-body-sm);
          color: var(--color-text-secondary);
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .noti-time {
          font-size: var(--text-caption);
          color: var(--color-text-tertiary);
        }
      `}</style>
    </div>
  );
}
