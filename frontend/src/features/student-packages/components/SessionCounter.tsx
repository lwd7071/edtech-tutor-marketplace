'use client';

import React from 'react';
import { Tooltip } from 'antd';

interface SessionCounterProps {
  remainingSessions: number;
  reservedSessions: number;
  completedSessions: number;
  refundedSessions: number;
  size?: 'small' | 'middle' | 'large';
}

/** Four API-provided portions of a StudentPackage's session balance. */
export const SessionCounter: React.FC<SessionCounterProps> = ({
  remainingSessions,
  reservedSessions,
  completedSessions,
  refundedSessions,
  size = 'middle',
}) => {
  const items = [
    { key: 'remaining', label: 'Còn lại', value: remainingSessions, color: 'var(--tm-ink)', tooltip: 'Số buổi khả dụng để học' },
    { key: 'reserved', label: 'Đang giữ', value: reservedSessions, color: 'var(--color-warning-600)', tooltip: 'Số buổi đang lên lịch chờ học' },
    { key: 'completed', label: 'Đã học', value: completedSessions, color: 'var(--color-success-600)', tooltip: 'Số buổi đã hoàn thành' },
    { key: 'refunded', label: 'Đã hoàn', value: refundedSessions, color: 'var(--color-text-tertiary)', tooltip: 'Số buổi đã được hoàn tiền' },
  ];

  return (
    <dl className={`tm-session-counter tm-session-counter-${size}`} aria-label="Phân bổ số buổi học">
      {items.map((item) => (
        <div className="tm-session-counter-item" key={item.key}>
          <dt>
            <Tooltip title={item.tooltip}><span tabIndex={0}>{item.label}</span></Tooltip>
          </dt>
          <dd style={{ color: item.color }}>{item.value}</dd>
        </div>
      ))}
    </dl>
  );
};
