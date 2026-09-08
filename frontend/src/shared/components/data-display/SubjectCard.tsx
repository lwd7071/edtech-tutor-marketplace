import React from 'react';
import { Card } from 'antd';
import { BookOutlined, TeamOutlined } from '@ant-design/icons';

interface SubjectCardProps {
  id: string;
  name: string;
  description?: string;
  teacherCount?: number;
  imageUrl?: string;
  onClick?: () => void;
  className?: string;
  isCompact?: boolean;
}

export default function SubjectCard({
  name,
  description,
  teacherCount = 0,
  imageUrl,
  onClick,
  className = '',
  isCompact = false,
}: SubjectCardProps) {
  return (
    <Card 
      hoverable={!!onClick} 
      onClick={onClick}
      className={`subject-card ${isCompact ? 'compact' : 'default'} ${className}`}
      styles={{ body: { padding: isCompact ? 'var(--space-3)' : 'var(--space-4)' } }}
    >
      <div className="icon-wrapper">
        {imageUrl ? (
          <img src={imageUrl} alt={name} className="subject-image" />
        ) : (
          <BookOutlined className="default-icon" />
        )}
      </div>
      <h3 className="subject-name">{name}</h3>
      {description && <p className="subject-desc">{description}</p>}
      
      <div className="teacher-count">
        <TeamOutlined />
        <span>{teacherCount > 0 ? `${teacherCount} gia sư` : 'Chưa có gia sư'}</span>
      </div>

      <style>{`
        .subject-card {
          border-radius: var(--radius-lg);
          border: var(--border-default);
          text-align: center;
          transition: all var(--duration-base) var(--ease-standard);
        }
        .subject-card:hover {
          border-color: var(--color-primary-500);
          transform: translateY(-2px);
        }
        .icon-wrapper {
          width: 48px;
          height: 48px;
          margin: 0 auto var(--space-3) auto;
          background-color: var(--color-primary-50);
          border-radius: var(--radius-md);
          display: flex;
          align-items: center;
          justify-content: center;
          color: var(--color-primary-600);
          font-size: 24px;
        }
        .subject-image {
          width: 100%;
          height: 100%;
          object-fit: cover;
          border-radius: var(--radius-md);
        }
        .subject-name {
          margin: 0 0 var(--space-2) 0;
          font-size: var(--text-h4);
          font-weight: var(--weight-bold);
          color: var(--color-text-primary);
        }
        .subject-desc {
          font-size: var(--text-body-sm);
          color: var(--color-text-secondary);
          margin: 0 0 var(--space-3) 0;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .teacher-count {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: var(--space-1);
          font-size: var(--text-caption);
          color: var(--color-text-tertiary);
        }
      `}</style>
    </Card>
  );
}
