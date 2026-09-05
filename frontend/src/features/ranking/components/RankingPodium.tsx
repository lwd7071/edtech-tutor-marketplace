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
    if (!teacher) return <div className="podium-empty flex-1" />;

    const isRank1 = position === 1;
    const heightClass = isRank1 ? 'h-40 md:h-48' : (position === 2 ? 'h-32 md:h-36' : 'h-28 md:h-32');
    const colorClass = isRank1 ? 'bg-amber-100 border-amber-300' : (position === 2 ? 'bg-slate-100 border-slate-300' : 'bg-orange-50 border-orange-200');
    const badgeColor = isRank1 ? '#f59e0b' : (position === 2 ? '#94a3b8' : '#d97706');

    return (
      <Link href={`/teachers/${teacher.teacherId}`} className="flex-1 flex flex-col items-center group transition-transform hover:-translate-y-2">
        <div className="relative mb-3">
          <Avatar 
            src={teacher.avatarUrl} 
            size={isRank1 ? 100 : 80} 
            className={`border-4 border-white shadow-md ${isRank1 ? 'shadow-amber-200' : ''}`}
          >
            {teacher.fullName.charAt(0)}
          </Avatar>
          <div 
            className="absolute -bottom-3 left-1/2 -translate-x-1/2 rounded-full px-3 py-0.5 text-white font-bold text-xs shadow-sm flex items-center gap-1"
            style={{ backgroundColor: badgeColor }}
          >
            {isRank1 && <TrophyOutlined />}
            Top {position}
          </div>
        </div>
        
        <div className="text-center mb-4 px-2">
          <div className="font-bold text-text-primary line-clamp-1 group-hover:text-primary-600 transition-colors">
            {teacher.fullName}
          </div>
          <div className="flex items-center justify-center gap-1 text-sm mt-1">
            <StarFilled className="text-amber-400" />
            <Text strong>{teacher.bayesianRating?.toFixed(1) || '0.0'}</Text>
            <Text type="secondary" className="text-xs">
              ({teacher.completedSessionCount} buổi)
            </Text>
          </div>
        </div>

        <div className={`w-full rounded-t-lg border-t border-l border-r flex items-end justify-center pb-4 ${heightClass} ${colorClass}`}>
          <span className="text-4xl font-black text-black/10">{position}</span>
        </div>
      </Link>
    );
  };

  return (
    <div className="ranking-podium max-w-3xl mx-auto mt-12 mb-16 px-4">
      <div className="flex items-end justify-center gap-2 md:gap-6">
        {renderPodiumItem(rank2, 2)}
        {renderPodiumItem(rank1, 1)}
        {renderPodiumItem(rank3, 3)}
      </div>
    </div>
  );
};
