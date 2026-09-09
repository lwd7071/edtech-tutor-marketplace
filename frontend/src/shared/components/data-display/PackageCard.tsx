import React from 'react';
import { Card, Progress } from 'antd';
import { PlayCircleOutlined } from '@ant-design/icons';
import { MoneyText } from './MoneyText';

interface PackageCardProps {
  id: string;
  name: string;
  price: number;
  sessionCount: number;
  description?: string;
  variant?: 'public' | 'purchased';
  completedSessions?: number; // Only for purchased
  onClick?: () => void;
  className?: string;
}

export default function PackageCard({
  name,
  price,
  sessionCount,
  description,
  variant = 'public',
  completedSessions = 0,
  onClick,
  className = '',
}: PackageCardProps) {
  const isPurchased = variant === 'purchased';
  const progressPercent = sessionCount > 0 ? Math.round((completedSessions / sessionCount) * 100) : 0;

  return (
    <Card 
      hoverable={!!onClick} 
      onClick={onClick}
      className={`package-card ${isPurchased ? 'purchased' : 'public'} ${className}`}
      styles={{ body: { padding: 'var(--space-4)', display: 'flex', flexDirection: 'column', height: '100%' } }}
    >
      <div className="pkg-header">
        <h3 className="pkg-name">{name}</h3>
        {!isPurchased && <div className="pkg-sessions">{sessionCount} buổi</div>}
      </div>

      {!isPurchased && description && (
        <p className="pkg-desc">{description}</p>
      )}

      {isPurchased ? (
        <div className="pkg-progress-area mt-auto">
          <div className="progress-text">
            <span>Đã học: {completedSessions}/{sessionCount} buổi</span>
          </div>
          <Progress percent={progressPercent} strokeColor="var(--color-primary-600)" railColor="var(--color-disabled-bg)" size="small" />
          <div className="play-cta">
            <PlayCircleOutlined /> Tiếp tục học
          </div>
        </div>
      ) : (
        <div className="pkg-price-area mt-auto">
          <MoneyText amount={price} className="pkg-price" />
        </div>
      )}

      <style>{`
        .package-card {
          border-radius: var(--radius-lg);
          border: var(--border-default);
          transition: all var(--duration-base) var(--ease-standard);
        }
        .package-card:hover {
          border-color: var(--color-primary-500);
          box-shadow: var(--shadow-sm);
        }
        .pkg-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          gap: var(--space-2);
          margin-bottom: var(--space-2);
        }
        .pkg-name {
          margin: 0;
          font-size: var(--text-h4);
          font-weight: var(--weight-semibold);
          color: var(--color-text-primary);
          line-height: 1.4;
        }
        .pkg-sessions {
          background-color: var(--color-primary-50);
          color: var(--color-primary-600);
          padding: 2px 8px;
          border-radius: var(--radius-full);
          font-size: var(--text-caption);
          font-weight: var(--weight-medium);
          white-space: nowrap;
        }
        .pkg-desc {
          font-size: var(--text-body-sm);
          color: var(--color-text-secondary);
          margin: 0 0 var(--space-4) 0;
          display: -webkit-box;
          -webkit-line-clamp: 3;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .mt-auto {
          margin-top: auto;
        }
        .pkg-price-area {
          padding-top: var(--space-3);
          border-top: var(--border-default);
        }
        .pkg-price {
          font-size: var(--text-h3);
          font-weight: var(--weight-bold);
          color: var(--color-text-primary);
        }
        
        /* Purchased variant */
        .pkg-progress-area {
          padding-top: var(--space-3);
        }
        .progress-text {
          display: flex;
          justify-content: space-between;
          font-size: var(--text-caption);
          color: var(--color-text-secondary);
          margin-bottom: var(--space-1);
        }
        .play-cta {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: var(--space-2);
          margin-top: var(--space-3);
          color: var(--color-primary-600);
          font-weight: var(--weight-medium);
          font-size: var(--text-body-sm);
        }
        .package-card.purchased:hover .play-cta {
          color: var(--color-primary-700);
        }
      `}</style>
    </Card>
  );
}
