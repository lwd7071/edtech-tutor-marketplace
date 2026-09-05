import React from 'react';
import { Card, Button, Typography } from 'antd';
import { HeartOutlined, HeartFilled } from '@ant-design/icons';
import { Avatar } from './Avatar';
import { RatingStars } from './RatingStars';
import { MoneyText } from './MoneyText';

const { Text } = Typography;

interface TeacherCardProps {
  id: string;
  name: string;
  avatarUrl?: string;
  isVerified?: boolean;
  subjects?: string[];
  rating?: number;
  reviewCount?: number;
  lowestPrice?: number;
  variant?: 'full' | 'compact';
  onClick?: () => void;
  className?: string;
}

export default function TeacherCard({
  name,
  avatarUrl,
  isVerified = false,
  subjects = [],
  rating = 0,
  reviewCount = 0,
  lowestPrice,
  variant = 'full',
  onClick,
  className = '',
}: TeacherCardProps) {
  const isCompact = variant === 'compact';

  return (
    <Card 
      hoverable={!!onClick} 
      onClick={onClick}
      className={`teacher-card ${isCompact ? 'compact' : 'full'} ${className}`}
      styles={{ body: { padding: isCompact ? 'var(--space-4)' : 'var(--space-5)' } }}
    >
      <div className="card-header">
        <Avatar src={avatarUrl} size={isCompact ? 'md' : 'lg'} isVerified={isVerified} />
        <div className="header-info">
          <h3 className="teacher-name">{name}</h3>
          <div className="subjects">
            {subjects.length > 0 ? subjects.join(', ') : 'Chưa cập nhật môn học'}
          </div>
        </div>
      </div>

      <div className="card-body">
        <div className="rating-row">
          <RatingStars value={rating} readonly />
          <span className="rating-value">{rating.toFixed(1)}</span>
          <Text type="secondary" style={{ fontSize: '12px', marginLeft: 4 }}>
            ({reviewCount})
          </Text>
        </div>

        {lowestPrice !== undefined && (
          <div className="price-row">
            <span className="price-label">Từ</span>
            <MoneyText amount={lowestPrice} className="price-value" />
            <span className="price-unit">/buổi</span>
          </div>
        )}
      </div>

      <style>{`
        .teacher-card {
          border-radius: var(--radius-lg);
          border: var(--border-default);
          transition: border-color var(--duration-base) var(--ease-standard), box-shadow var(--duration-base) var(--ease-standard);
        }
        .teacher-card:hover {
          border-color: var(--color-primary-500);
        }
        .card-header {
          display: flex;
          align-items: center;
          gap: var(--space-4);
          margin-bottom: var(--space-4);
        }
        .teacher-name {
          margin: 0;
          font-size: var(--text-h4);
          font-weight: var(--weight-bold);
          color: var(--color-text-primary);
          line-height: 1.4;
        }
        .subjects {
          font-size: var(--text-body-sm);
          color: var(--color-text-secondary);
          margin-top: 2px;
          display: -webkit-box;
          -webkit-line-clamp: 1;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .card-body {
          display: flex;
          flex-direction: column;
          gap: var(--space-2);
        }
        .rating-row {
          display: flex;
          align-items: center;
          gap: var(--space-2);
          font-size: var(--text-body-sm);
        }
        .rating-value {
          font-weight: var(--weight-semibold);
          color: var(--color-text-primary);
        }
        .review-count {
          color: var(--color-text-tertiary);
        }
        .price-row {
          display: flex;
          align-items: baseline;
          gap: var(--space-1);
          margin-top: var(--space-2);
        }
        .price-label, .price-unit {
          font-size: var(--text-caption);
          color: var(--color-text-secondary);
        }
        .price-value {
          font-size: var(--text-body-lg);
          font-weight: var(--weight-bold);
          color: var(--color-primary-600);
        }

        /* Compact overrides */
        .teacher-card.compact .card-header {
          margin-bottom: var(--space-3);
        }
        .teacher-card.compact .teacher-name {
          font-size: var(--text-body);
        }
        .teacher-card.compact .price-row {
          margin-top: 0;
        }
        .teacher-card.compact .price-value {
          font-size: var(--text-body);
        }
      `}</style>
    </Card>
  );
}
