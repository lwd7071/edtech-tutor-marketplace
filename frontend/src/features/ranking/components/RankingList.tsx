'use client';

import { Avatar } from 'antd';
import { StarFilled } from '@ant-design/icons';
import Link from 'next/link';
import type { TeacherRankingItem } from '@/shared/api/public';

interface RankingListProps {
  teachers: TeacherRankingItem[];
}

export const RankingList = ({ teachers }: RankingListProps) => {
  if (teachers.length === 0) {
    return <p className="tm-ranking-empty" role="status">Chưa có dữ liệu xếp hạng</p>;
  }

  return (
    <ol className="tm-ranking-list" aria-label="Xếp hạng gia sư">
      {teachers.map(teacher => (
        <li className="tm-ranking-entry" key={teacher.teacherId}>
          <Link className="tm-ranking-link" href={`/teachers/${teacher.teacherId}`}>
            <span className="tm-ranking-position" aria-label={`Hạng ${teacher.globalRank ?? 'chưa xếp'}`}>
              {teacher.globalRank ?? '—'}
            </span>
            <Avatar className="tm-ranking-avatar" src={teacher.avatarUrl} size={48} aria-hidden="true">
              {teacher.fullName.charAt(0)}
            </Avatar>
            <span className="tm-ranking-profile">
              <strong className="tm-ranking-name">{teacher.fullName}</strong>
              {teacher.bioExcerpt && <span className="tm-ranking-bio">{teacher.bioExcerpt}</span>}
            </span>
            <span className="tm-ranking-metrics">
              <span className="tm-ranking-rating" aria-label={`Điểm đánh giá ${teacher.bayesianRating?.toFixed(1) || '0.0'}`}>
                <StarFilled aria-hidden="true" />
                {teacher.bayesianRating?.toFixed(1) || '0.0'}
              </span>
              <span className="tm-ranking-sessions">
                {teacher.completedSessionCount ?? '—'} buổi
              </span>
            </span>
          </Link>
        </li>
      ))}
    </ol>
  );
};
