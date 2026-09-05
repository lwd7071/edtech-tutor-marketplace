import React from 'react';
import { Result, Button, Typography } from 'antd';

const { Text } = Typography;

export type ErrorStateVariant = 'inline' | 'page' | 'auth';

export interface ErrorStateProps {
  variant?: ErrorStateVariant;
  error?: Error;
  title?: string;
  message?: React.ReactNode;
  onRetry?: () => void;
  actionText?: string;
  trackingId?: string;
  style?: React.CSSProperties;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  variant = 'page',
  error,
  title = 'Đã có lỗi xảy ra',
  message = 'Chúng tôi không thể hoàn thành yêu cầu của bạn lúc này.',
  onRetry,
  actionText,
  trackingId,
  style,
}) => {
  const isInline = variant === 'inline';

  const extraContent = (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16 }}>
      {onRetry && (
        <Button type="primary" onClick={onRetry}>
          {actionText || 'Thử lại'}
        </Button>
      )}
      {trackingId && (
        <Text type="secondary" style={{ fontSize: '12px', opacity: 0.7 }}>
          Tracking ID: {trackingId}
        </Text>
      )}
    </div>
  );

  if (isInline) {
    return (
      <div style={{ textAlign: 'center', padding: '16px', ...style }}>
        <Text type="danger" strong>{title}</Text>
        <div style={{ margin: '8px 0' }}>
          <Text type="secondary">{message}</Text>
        </div>
        {extraContent}
      </div>
    );
  }

  const status = variant === 'auth' ? '403' : '500';

  return (
    <Result
      status={status}
      title={title}
      subTitle={message}
      extra={extraContent}
      style={style}
    />
  );
};
