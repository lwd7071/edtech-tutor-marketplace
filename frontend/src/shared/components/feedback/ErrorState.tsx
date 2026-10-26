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
  className?: string;
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
  className,
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
      <section className={className} aria-label="Thông báo lỗi" style={{ textAlign: 'center', padding: '16px', ...style }}>
        <h2 className="tm-error-inline-title">{title}</h2>
        <p className="tm-error-inline-message">{message}</p>
        {extraContent}
      </section>
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
      className={className}
    />
  );
};
