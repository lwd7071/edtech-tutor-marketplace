'use client';

import { Avatar } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import type { TeacherCard } from '@/shared/api/public';

export function FeaturedTeachersVisual({ teachers }: { teachers: TeacherCard[] }) {
  const [leadTeacher, secondTeacher] = teachers;

  if (!leadTeacher) {
    return (
      <div className="tm-hero-visual-empty">
        <UserOutlined aria-hidden="true" />
        <span>Gia sư phù hợp bắt đầu từ mục tiêu học của bạn</span>
      </div>
    );
  }

  return (
    <>
      <div className="tm-hero-photo">
        <Avatar src={leadTeacher.avatarUrl ?? undefined} alt={leadTeacher.fullName} icon={<UserOutlined />}>
          {leadTeacher.fullName.slice(0, 1)}
        </Avatar>
      </div>
      <article className="tm-hero-teacher-card">
        <Avatar src={leadTeacher.avatarUrl ?? undefined} alt="" size={48}>
          {leadTeacher.fullName.slice(0, 1)}
        </Avatar>
        <div>
          <strong>{leadTeacher.fullName}</strong>
          <span>{leadTeacher.subjects.slice(0, 2).map(subject => subject.name).join(' · ') || `${leadTeacher.yearsOfExperience} năm kinh nghiệm`}</span>
        </div>
        {leadTeacher.reviewCount > 0 && <b>{leadTeacher.averageRating.toFixed(1)} ★</b>}
      </article>
      {secondTeacher && (
        <div className="tm-hero-secondary-photo">
          <Avatar src={secondTeacher.avatarUrl ?? undefined} alt={secondTeacher.fullName} size={112} icon={<UserOutlined />}>
            {secondTeacher.fullName.slice(0, 1)}
          </Avatar>
          <span>Gia sư Tutor Match</span>
        </div>
      )}
    </>
  );
}
