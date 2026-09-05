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
    <div className="ranking-list max-w-3xl mx-auto flex flex-col gap-3">
      {teachers.map((teacher) => (
        <Link 
          href={`/teachers/${teacher.teacherId}`} 
          key={teacher.teacherId}
          className="flex items-center p-4 bg-surface rounded-xl border border-border shadow-sm hover:shadow-md transition-shadow group"
        >
          <div className="w-12 font-bold text-xl text-text-tertiary text-center mr-4">
            {teacher.globalRank}
          </div>
          
          <Avatar 
            src={teacher.avatarUrl} 
            size={56} 
            className="border-2 border-border mr-4"
          >
            {teacher.fullName.charAt(0)}
          </Avatar>
          
          <div className="flex-1 min-w-0">
            <h3 className="font-bold text-text-primary text-lg truncate group-hover:text-primary-600 transition-colors">
              {teacher.fullName}
            </h3>
            {teacher.bioExcerpt && (
              <p className="text-text-secondary text-sm truncate mt-0.5">
                {teacher.bioExcerpt}
              </p>
            )}
          </div>
          
          <div className="flex flex-col items-end justify-center ml-4">
            <div className="flex items-center gap-1 font-bold text-lg">
              <StarFilled className="text-amber-400" />
              <span>{teacher.bayesianRating?.toFixed(1) || '0.0'}</span>
            </div>
            <div className="text-xs text-text-tertiary">
              {teacher.completedSessionCount} buổi
            </div>
          </div>
        </Link>
      ))}
    </div>
  );
};
