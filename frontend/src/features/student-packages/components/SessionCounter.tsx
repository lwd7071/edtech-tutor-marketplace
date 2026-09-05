'use client';

import React from 'react';
import { Space, Typography, Tooltip } from 'antd';

interface SessionCounterProps {
  remainingSessions: number;
  reservedSessions: number;
  completedSessions: number;
  refundedSessions: number;
  size?: 'small' | 'middle' | 'large';
}

/**
 * SessionCounter: Hiển thị bộ 4 chỉ số buổi học theo chuẩn SPEC-FE:
 * Còn lại / Đang giữ / Đã học / Đã hoàn
 */
export const SessionCounter: React.FC<SessionCounterProps> = ({
  remainingSessions,
  reservedSessions,
  completedSessions,
  refundedSessions,
  size = 'middle',
}) => {
  const isSmall = size === 'small';
  const isLarge = size === 'large';

  const numFontSize = isLarge ? 24 : isSmall ? 16 : 20;
  const labelFontSize = isLarge ? 13 : isSmall ? 11 : 12;

  const items = [
    {
      key: 'remaining',
      label: 'Còn lại',
      value: remainingSessions,
      color: 'var(--color-primary-600, #0F766E)',
      bg: 'var(--color-primary-50, #F0FDFA)',
      tooltip: 'Số buổi khả dụng để học',
    },
    {
      key: 'reserved',
      label: 'Đang giữ',
      value: reservedSessions,
      color: 'var(--color-warning-600, #B45309)',
      bg: 'var(--color-warning-bg, #FFFBEB)',
      tooltip: 'Số buổi đang lên lịch chờ học',
    },
    {
      key: 'completed',
      label: 'Đã học',
      value: completedSessions,
      color: 'var(--color-success-600, #15803D)',
      bg: 'var(--color-success-bg, #F0FDF4)',
      tooltip: 'Số buổi đã hoàn thành',
    },
    {
      key: 'refunded',
      label: 'Đã hoàn',
      value: refundedSessions,
      color: 'var(--color-text-tertiary, #8A837B)',
      bg: 'var(--color-surface-sunken, #F5F3EF)',
      tooltip: 'Số buổi đã được hoàn tiền',
    },
  ];

  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(4, 1fr)',
        gap: isSmall ? 6 : 8,
        width: '100%',
      }}
    >
      {items.map((item) => (
        <Tooltip title={item.tooltip} key={item.key}>
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              padding: isSmall ? '4px 6px' : '8px 10px',
              backgroundColor: item.bg,
              borderRadius: 'var(--radius-sm, 6px)',
              border: '1px solid var(--color-border, #E7E3DC)',
              textAlign: 'center',
            }}
          >
            <span
              style={{
                fontSize: numFontSize,
                fontWeight: 700,
                color: item.color,
                fontVariantNumeric: 'tabular-nums',
                lineHeight: 1.2,
              }}
            >
              {item.value}
            </span>
            <span
              style={{
                fontSize: labelFontSize,
                color: 'var(--color-text-secondary, #57534E)',
                marginTop: 2,
                whiteSpace: 'nowrap',
              }}
            >
              {item.label}
            </span>
          </div>
        </Tooltip>
      ))}
    </div>
  );
};
