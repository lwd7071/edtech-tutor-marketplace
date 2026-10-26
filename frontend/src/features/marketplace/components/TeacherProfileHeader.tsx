'use client';

import { StarFilled } from '@ant-design/icons';
import type { TeacherPublicDetail } from '@/shared/api/public';
import { Avatar } from '@/shared/components/data-display/Avatar';

export function TeacherProfileHeader({ teacher }: { teacher: TeacherPublicDetail }) {
  const location = [teacher.wardName, teacher.provinceName].filter(Boolean).join(', ');
  const teachingModes = [
    teacher.supportsOnline && 'Online',
    teacher.supportsOffline && 'Trực tiếp',
  ].filter(Boolean);

  return (
    <header className="tm-profile-sheet" aria-label={`Hồ sơ gia sư ${teacher.fullName}`}>
      <div className="tm-profile-portrait">
        <Avatar src={teacher.avatarUrl} alt={teacher.fullName} size="xl" />
      </div>
      <div className="tm-profile-overview">
        <h1 className="tm-profile-name">{teacher.fullName}</h1>
        {teacher.subjects.length > 0 && (
          <ul className="tm-profile-subjects" aria-label="Môn giảng dạy">
            {teacher.subjects.map(subject => <li className="tm-chip" key={subject}>{subject}</li>)}
          </ul>
        )}
        {teacher.credentials && teacher.credentials.length > 0 && (
          <ul className="tm-profile-credentials" aria-label="Minh chứng đã được duyệt">
            {teacher.credentials.map(credential => (
              <li key={credential.id}>
                <span className="tm-credential-approved">Đã xác minh</span>
                {credential.label}
              </li>
            ))}
          </ul>
        )}
        <dl className="tm-profile-facts">
          <div>
            <dt>Kinh nghiệm</dt>
            <dd>{teacher.yearsOfExperience} năm</dd>
          </div>
          {teachingModes.length > 0 && (
            <div>
              <dt>Hình thức</dt>
              <dd>{teachingModes.join(' · ')}</dd>
            </div>
          )}
          {location && (
            <div>
              <dt>Nơi ở</dt>
              <dd>{location} <span className="tm-profile-note">(gia sư tự khai)</span></dd>
            </div>
          )}
          {teacher.languages.length > 0 && (
            <div>
              <dt>Ngôn ngữ giảng dạy</dt>
              <dd>{teacher.languages.join(', ')}</dd>
            </div>
          )}
          <div>
            <dt>Đánh giá</dt>
            <dd>
              {teacher.reviewCount > 0 ? (
                <><StarFilled aria-hidden="true" /> {teacher.averageRating.toFixed(1)} · {teacher.reviewCount} đánh giá</>
              ) : 'Chưa có đánh giá'}
            </dd>
          </div>
        </dl>
      </div>
    </header>
  );
}
