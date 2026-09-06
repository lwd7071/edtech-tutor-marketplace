'use client';

import React from 'react';
import { TeacherRankingItem } from '@/shared/api/public';
import { Avatar, Typography } from 'antd';
import { StarFilled, TrophyOutlined } from '@ant-design/icons';
import Link from 'next/link';

const { Text } = Typography;

interface RankingPodiumProps {
  teachers: TeacherRankingItem[];
}

export const RankingPodium: React.FC<RankingPodiumProps> = ({ teachers }) => {
  if (!teachers || teachers.length === 0) return null;

  // We want to show top 3.
  const top3 = teachers.slice(0, 3);
  
  // To display them in a podium, the order should be: Rank 2, Rank 1, Rank 3.
  // We need to carefully handle cases where there are less than 3 teachers.
  const rank1 = top3.find(t => t.globalRank === 1) || top3[0];
  const rank2 = top3.find(t => t.globalRank === 2) || (top3.length > 1 ? top3[1] : null);
  const rank3 = top3.find(t => t.globalRank === 3) || (top3.length > 2 ? top3[2] : null);

  const renderPodiumItem = (teacher: TeacherRankingItem | null, position: 1 | 2 | 3) => {
    if (!teacher) return <div style={{ flex: 1 }} />;

    const isRank1 = position === 1;
    const height = isRank1 ? '160px' : (position === 2 ? '130px' : '110px');
    const bgColor = isRank1 ? 'var(--color-warning-bg)' : (position === 2 ? '#f1f5f9' : '#fff7ed');
    const borderColor = isRank1 ? 'var(--color-warning-600)' : (position === 2 ? '#cbd5e1' : '#fed7aa');
    const badgeColor = isRank1 ? 'var(--color-warning-600)' : (position === 2 ? '#94a3b8' : '#d97706');

    return (
      <Link href={`/teachers/${teacher.teacherId}`} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', textDecoration: 'none', transition: 'transform 0.2s', cursor: 'pointer' }}>
        <div style={{ position: 'relative', marginBottom: 'var(--space-3)' }}>
          <Avatar 
            src={teacher.avatarUrl} 
            size={isRank1 ? 100 : 80} 
            style={{ border: '4px solid white', boxShadow: isRank1 ? '0 4px 12px rgba(245, 158, 11, 0.3)' : 'var(--shadow-md)' }}
          >
            {teacher.fullName.charAt(0)}
          </Avatar>
          <div 
            style={{ 
              position: 'absolute', bottom: '-12px', left: '50%', transform: 'translateX(-50%)', 
              backgroundColor: badgeColor, borderRadius: 'var(--radius-full)', 
              padding: '2px 12px', color: 'white', fontWeight: 'bold', fontSize: '12px', 
              boxShadow: 'var(--shadow-sm)', display: 'flex', alignItems: 'center', gap: '4px',
              whiteSpace: 'nowrap'
            }}
          >
            {isRank1 && <TrophyOutlined />}
            Top {position}
          </div>
        </div>
        
        <div style={{ textAlign: 'center', marginBottom: 'var(--space-4)', padding: '0 8px' }}>
          <div style={{ fontWeight: 'bold', color: 'var(--color-text-primary)', textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap', maxWidth: '120px' }}>
            {teacher.fullName}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px', fontSize: '14px', marginTop: '4px' }}>
            <StarFilled style={{ color: '#f59e0b' }} />
            <Text strong>{teacher.bayesianRating?.toFixed(1) || '0.0'}</Text>
            <Text type="secondary" style={{ fontSize: '12px' }}>
              ({teacher.completedSessionCount} buổi)
            </Text>
          </div>
        </div>

        <div style={{ 
          width: '100%', borderTopLeftRadius: 'var(--radius-lg)', borderTopRightRadius: 'var(--radius-lg)', 
          borderTop: `1px solid ${borderColor}`, borderLeft: `1px solid ${borderColor}`, borderRight: `1px solid ${borderColor}`, 
          display: 'flex', alignItems: 'flex-end', justifyContent: 'center', paddingBottom: 'var(--space-4)',
          height, backgroundColor: bgColor
        }}>
          <span style={{ fontSize: '40px', fontWeight: 900, color: 'rgba(0,0,0,0.1)' }}>{position}</span>
        </div>
      </Link>
    );
  };

  return (
    <div style={{ maxWidth: '768px', margin: 'var(--space-12) auto var(--space-16)', padding: '0 var(--space-4)' }}>
      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'center', gap: 'var(--space-4)' }}>
        {renderPodiumItem(rank2, 2)}
        {renderPodiumItem(rank1, 1)}
        {renderPodiumItem(rank3, 3)}
      </div>
    </div>
  );
};
