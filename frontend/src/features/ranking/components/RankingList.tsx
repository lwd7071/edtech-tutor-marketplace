'use client';

import React from 'react';
import { TeacherRankingItem } from '@/shared/api/public';
import { Avatar, Typography } from 'antd';
import { StarFilled } from '@ant-design/icons';
import Link from 'next/link';

const { Text } = Typography;

interface RankingListProps {
  teachers: TeacherRankingItem[];
}

export const RankingList: React.FC<RankingListProps> = ({ teachers }) => {
  if (!teachers || teachers.length === 0) {
    return (
      <div className="text-center py-10 text-text-tertiary">
        Chưa có dữ liệu xếp hạng
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '768px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
      {teachers.map((teacher) => (
        <Link 
          href={`/teachers/${teacher.teacherId}`} 
          key={teacher.teacherId}
          style={{ 
            display: 'flex', alignItems: 'center', padding: 'var(--space-4)', 
            backgroundColor: 'var(--color-surface)', borderRadius: 'var(--radius-xl)', 
            border: '1px solid var(--color-border)', boxShadow: 'var(--shadow-sm)',
            textDecoration: 'none', color: 'inherit', transition: 'box-shadow 0.2s'
          }}
          onMouseEnter={(e) => (e.currentTarget.style.boxShadow = 'var(--shadow-md)')}
          onMouseLeave={(e) => (e.currentTarget.style.boxShadow = 'var(--shadow-sm)')}
        >
          <div style={{ width: '48px', fontWeight: 'bold', fontSize: '20px', color: 'var(--color-text-tertiary)', textAlign: 'center', marginRight: 'var(--space-4)' }}>
            {teacher.globalRank}
          </div>
          
          <Avatar 
            src={teacher.avatarUrl} 
            size={56} 
            style={{ border: '2px solid var(--color-border)', marginRight: 'var(--space-4)' }}
          >
            {teacher.fullName.charAt(0)}
          </Avatar>
          
          <div style={{ flex: 1, minWidth: 0 }}>
            <h3 style={{ margin: 0, fontWeight: 'bold', color: 'var(--color-text-primary)', fontSize: '18px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {teacher.fullName}
            </h3>
            {teacher.bioExcerpt && (
              <p style={{ margin: '2px 0 0', color: 'var(--color-text-secondary)', fontSize: '14px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {teacher.bioExcerpt}
              </p>
            )}
          </div>
          
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', justifyContent: 'center', marginLeft: 'var(--space-4)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 'bold', fontSize: '18px' }}>
              <StarFilled style={{ color: '#f59e0b' }} />
              <span>{teacher.bayesianRating?.toFixed(1) || '0.0'}</span>
            </div>
            <div style={{ fontSize: '12px', color: 'var(--color-text-tertiary)' }}>
              {teacher.completedSessionCount} buổi
            </div>
          </div>
        </Link>
      ))}
    </div>
  );
};
