import React from 'react';
import { Space, Tooltip } from 'antd';
import { QuestionCircleOutlined } from '@ant-design/icons';

export interface SessionCounterSimpleProps {
  remaining: number;
  onHold: number;
  completed: number;
  total: number;
  variant?: 'horizontal' | 'compact';
}

export const SessionCounterSimple: React.FC<SessionCounterSimpleProps> = ({
  remaining,
  onHold,
  completed,
  total,
  variant = 'horizontal',
}) => {
  if (variant === 'compact') {
    return (
      <Tooltip 
        title={
          <div>
            <div>Còn lại: {remaining}</div>
            <div>Đang giữ: {onHold}</div>
            <div>Đã học: {completed}</div>
          </div>
        }
      >
        <span style={{ cursor: 'help' }}>{remaining} / {total}</span>
      </Tooltip>
    );
  }

  return (
    <Space size="middle" wrap>
      <span>Còn lại: <strong>{remaining}</strong></span>
      <Tooltip title="Số buổi học đang được giữ chỗ nhưng chưa diễn ra.">
        <span style={{ cursor: 'help', color: 'var(--color-warning-600)' }}>
          Đang giữ: <strong>{onHold}</strong> <QuestionCircleOutlined style={{ fontSize: '12px' }} />
        </span>
      </Tooltip>
      <span>Đã học: <strong>{completed}</strong></span>
      <span>Tổng: <strong>{total}</strong></span>
    </Space>
  );
};
