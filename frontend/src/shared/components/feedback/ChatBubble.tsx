import React from 'react';
import { CheckCircleOutlined, SyncOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { DateTimeText } from '../data-display/DateTimeText';
import { Tooltip } from 'antd';

export interface ChatBubbleProps {
  id?: string;
  content: string;
  variant: 'own' | 'other' | 'system';
  timestamp?: string;
  status?: 'sending' | 'sent' | 'failed';
  className?: string;
}

export default function ChatBubble({
  content,
  variant,
  timestamp,
  status = 'sent',
  className = '',
}: ChatBubbleProps) {
  
  if (variant === 'system') {
    return (
      <div className={`chat-bubble system ${className}`}>
        <span className="system-text">{content}</span>
        <style>{`
          .chat-bubble.system {
            display: flex;
            justify-content: center;
            margin: var(--space-4) 0;
          }
          .system-text {
            background-color: var(--color-surface-sunken);
            color: var(--color-text-tertiary);
            padding: 2px 12px;
            border-radius: var(--radius-full);
            font-size: var(--text-caption);
          }
        `}</style>
      </div>
    );
  }

  const isOwn = variant === 'own';

  return (
    <div className={`chat-bubble ${variant} ${className}`}>
      <div className="bubble-wrapper">
        <div className="bubble-content">
          {content.split('\\n').map((line, i) => (
            <React.Fragment key={i}>
              {line}
              <br />
            </React.Fragment>
          ))}
        </div>
        
        <div className="bubble-meta">
          {timestamp && (
            <span className="timestamp">
              <DateTimeText value={timestamp} variant="time" style={{ fontSize: '11px', color: 'inherit', opacity: 0.8 }} />
            </span>
          )}
          {isOwn && (
            <span className={`status-icon ${status}`}>
              {status === 'sending' && <SyncOutlined spin />}
              {status === 'sent' && <CheckCircleOutlined />}
              {status === 'failed' && (
                <Tooltip title="Gửi thất bại. Nhấn để thử lại.">
                  <ExclamationCircleOutlined className="error" />
                </Tooltip>
              )}
            </span>
          )}
        </div>
      </div>

      <style>{`
        .chat-bubble {
          display: flex;
          margin-bottom: var(--space-3);
          width: 100%;
        }
        .chat-bubble.own {
          justify-content: flex-end;
        }
        .chat-bubble.other {
          justify-content: flex-start;
        }
        
        .bubble-wrapper {
          max-width: 75%;
          display: flex;
          flex-direction: column;
        }
        .chat-bubble.own .bubble-wrapper {
          align-items: flex-end;
        }
        .chat-bubble.other .bubble-wrapper {
          align-items: flex-start;
        }
        
        .bubble-content {
          padding: 8px 12px;
          font-size: var(--text-body);
          line-height: 1.5;
          word-break: break-word;
        }
        .chat-bubble.own .bubble-content {
          background-color: var(--color-primary-600);
          color: var(--color-text-inverse);
          border-radius: 16px 16px 4px 16px;
        }
        .chat-bubble.other .bubble-content {
          background-color: var(--color-surface-sunken);
          color: var(--color-text-primary);
          border-radius: 16px 16px 16px 4px;
        }
        
        .bubble-meta {
          display: flex;
          align-items: center;
          gap: var(--space-1);
          margin-top: 4px;
          font-size: var(--text-caption);
          color: var(--color-text-tertiary);
        }
        .status-icon.failed .error {
          color: var(--color-error-600);
          cursor: pointer;
        }
      `}</style>
    </div>
  );
}
