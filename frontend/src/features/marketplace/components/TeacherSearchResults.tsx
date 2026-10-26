'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Skeleton } from 'antd';
import { EnvironmentOutlined, StarFilled } from '@ant-design/icons';
import { EmptyState } from '@/shared/components/feedback/EmptyState';
import { ErrorState } from '@/shared/components/feedback/ErrorState';
import { Avatar } from '@/shared/components/data-display/Avatar';
import type { TeacherCard } from '@/shared/api/public';

const money = (value: number) => new Intl.NumberFormat('vi-VN').format(value);

function QuickPreview({ teacher, mobile = false, onClose }: { teacher: TeacherCard; mobile?: boolean; onClose?: () => void }) {
  const location = [teacher.wardName, teacher.provinceName].filter(Boolean).join(', ');
  return (
    <section id={mobile ? `teacher-preview-${teacher.id}` : undefined} className={`tm-teacher-preview${mobile ? ' tm-teacher-preview-mobile' : ''}`} aria-label={`Xem nhanh hồ sơ ${teacher.fullName}`}>
      <header className="tm-teacher-preview-top">
        <figure className="tm-preview-identity">
          <Avatar src={teacher.avatarUrl ?? undefined} alt={teacher.fullName} size={mobile ? 'md' : 'lg'} isVerified={teacher.verifiedBadge} />
          <figcaption><strong>{teacher.fullName}</strong><span>{teacher.yearsOfExperience} năm kinh nghiệm</span></figcaption>
        </figure>
        {mobile && <button type="button" className="tm-preview-close" onClick={onClose} aria-label="Đóng xem nhanh">Đóng</button>}
      </header>
      {teacher.bioExcerpt && <p className="tm-teacher-preview-bio">{teacher.bioExcerpt}</p>}
      <ul className="tm-teacher-preview-facts">
        <li><span>Đánh giá</span><strong>{teacher.reviewCount ? <>{teacher.averageRating.toFixed(1)} <StarFilled aria-hidden="true" /> · {teacher.reviewCount}</> : 'Chưa có đánh giá'}</strong></li>
        <li><span>Giá gói</span><strong>{teacher.startingPriceVnd > 0 ? `Từ ${money(teacher.startingPriceVnd)}đ` : 'Chưa mở gói'}</strong></li>
        <li><span>Hình thức</span><strong>{[teacher.supportsOnline && 'Online', teacher.supportsOffline && 'Trực tiếp'].filter(Boolean).join(' · ') || 'Chưa cập nhật'}</strong></li>
        {location && <li><span>Nơi ở</span><strong><EnvironmentOutlined aria-hidden="true" /> {location}</strong></li>}
      </ul>
      {teacher.subjects.length > 0 && <ul className="tm-teacher-preview-subjects" aria-label="Môn giảng dạy">{teacher.subjects.slice(0, 4).map(subject => <li className="tm-chip" key={subject.id}>{subject.name}</li>)}</ul>}
      <Link className="tm-preview-link" href={`/teachers/${teacher.id}`}>Xem hồ sơ và lịch rảnh</Link>
    </section>
  );
}

export function TeacherSearchResults({ teachers, isLoading, isError, onRetry }: {
  teachers: TeacherCard[];
  isLoading?: boolean;
  isError?: boolean;
  onRetry?: () => void;
}) {
  const [activeId, setActiveId] = useState<string>();
  const activeTeacher = teachers.find(teacher => teacher.id === activeId);

  if (isError) return <ErrorState title="Chưa tải được danh sách gia sư" actionText="Tải lại" onRetry={onRetry} />;
  if (isLoading) return <div className="tm-search-results-layout" role="status" aria-label="Đang tải danh sách gia sư"><ul className="tm-search-result-list">{[1, 2, 3].map(index => <li className="tm-search-result-skeleton" key={index}><Skeleton active avatar /></li>)}</ul></div>;
  if (!teachers.length) return <EmptyState title="Chưa tìm thấy gia sư phù hợp" description="Thử bỏ bớt bộ lọc hoặc chọn môn học khác." />;

  return (
    <div className="tm-search-results-layout">
      <ul className="tm-search-result-list" aria-label="Danh sách gia sư">
        {teachers.map(teacher => {
          const selected = activeId === teacher.id;
          const location = [teacher.wardName, teacher.provinceName].filter(Boolean).join(', ');
          return (
            <li className={`tm-search-result${selected ? ' is-active' : ''}`} key={teacher.id} onMouseEnter={() => setActiveId(teacher.id)}>
              <article className="tm-search-result-record">
                <div className="tm-result-profile">
                  <Avatar src={teacher.avatarUrl ?? undefined} alt={teacher.fullName} size="lg" isVerified={teacher.verifiedBadge}>{teacher.fullName.slice(0, 1)}</Avatar>
                  <div className="tm-search-result-content">
                    <div className="tm-search-result-heading">
                      <button type="button" id={`teacher-card-${teacher.id}`} className="tm-search-result-select" aria-label={`Xem nhanh ${teacher.fullName}`} aria-expanded={selected} aria-controls={`teacher-preview-${teacher.id}`} onFocus={() => setActiveId(teacher.id)} onClick={() => setActiveId(teacher.id)}>
                        <strong>{teacher.fullName}</strong>
                      </button>
                      {teacher.verifiedBadge && <span className="tm-verified-label">Đã xác minh</span>}
                    </div>
                    {teacher.subjects.length > 0 && <ul className="tm-search-result-subjects">{teacher.subjects.slice(0, 3).map(subject => <li className="tm-chip" key={subject.id}>{subject.name}</li>)}</ul>}
                    {teacher.bioExcerpt && <p className="tm-search-result-bio">{teacher.bioExcerpt}</p>}
                    <ul className="tm-search-result-meta" aria-label="Thông tin gia sư">
                      <li><span>Đánh giá</span><strong>{teacher.reviewCount > 0 ? <><StarFilled aria-hidden="true" /> {teacher.averageRating.toFixed(1)} · {teacher.reviewCount}</> : 'Chưa có đánh giá'}</strong></li>
                      <li><span>Kinh nghiệm</span><strong>{teacher.yearsOfExperience} năm</strong></li>
                      {location && <li><span>Nơi ở</span><strong><EnvironmentOutlined aria-hidden="true" /> {location}</strong></li>}
                    </ul>
                  </div>
                </div>
                <aside className="tm-search-result-offer">
                  <span>Giá gói</span>
                  <strong>{teacher.startingPriceVnd > 0 ? `${money(teacher.startingPriceVnd)}đ` : 'Chưa mở gói'}</strong>
                  {teacher.startingPriceVnd > 0 && <small>trọn gói</small>}
                  <Link href={`/teachers/${teacher.id}`}>Xem hồ sơ</Link>
                </aside>
              </article>
              {selected && <QuickPreview teacher={teacher} mobile onClose={() => { setActiveId(undefined); document.getElementById(`teacher-card-${teacher.id}`)?.focus(); }} />}
            </li>
          );
        })}
      </ul>
      <aside className="tm-search-preview-aside" aria-live="polite" aria-label="Xem nhanh gia sư">
        {activeTeacher ? <QuickPreview teacher={activeTeacher} /> : <div className="tm-search-preview-prompt"><strong>Thông tin gia sư</strong><span>Chọn một hồ sơ trong danh sách để xem nhanh.</span></div>}
      </aside>
    </div>
  );
}
