'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Skeleton } from 'antd';
import { ArrowRightOutlined, EnvironmentOutlined, StarFilled, VideoCameraOutlined } from '@ant-design/icons';
import { EmptyState } from '@/shared/components/feedback/EmptyState';
import { ErrorState } from '@/shared/components/feedback/ErrorState';
import { Avatar } from '@/shared/components/data-display/Avatar';
import type { TeacherCard } from '@/shared/api/public';

const money = (value: number) => new Intl.NumberFormat('vi-VN').format(value);

function QuickPreview({ teacher, mobile = false, onClose }: { teacher: TeacherCard; mobile?: boolean; onClose?: () => void }) {
  const location = [teacher.wardName, teacher.provinceName].filter(Boolean).join(', ');
  return (
    <section id={mobile ? `teacher-preview-${teacher.id}` : undefined} className={`tm-teacher-preview${mobile ? ' tm-teacher-preview-mobile' : ''}`} aria-label={`Xem nhanh hồ sơ ${teacher.fullName}`}>
      <div className="tm-teacher-preview-top">
        <Avatar src={teacher.avatarUrl ?? undefined} alt={teacher.fullName} size={mobile ? 'md' : 'lg'} isVerified={teacher.verifiedBadge} />
        <div>
          <h3>{teacher.fullName}</h3>
          <span>{teacher.yearsOfExperience} năm kinh nghiệm</span>
        </div>
        {mobile && <button type="button" className="tm-preview-close" onClick={onClose} aria-label="Đóng xem nhanh">×</button>}
      </div>
      {teacher.bioExcerpt && <p className="tm-teacher-preview-bio">{teacher.bioExcerpt}</p>}
      <dl className="tm-teacher-preview-facts">
        <div><dt>Đánh giá</dt><dd>{teacher.reviewCount ? <>{teacher.averageRating.toFixed(1)} <StarFilled aria-hidden="true" /> · {teacher.reviewCount}</> : 'Chưa có đánh giá'}</dd></div>
        <div><dt>Giá gói</dt><dd>{teacher.startingPriceVnd > 0 ? `Từ ${money(teacher.startingPriceVnd)}đ` : 'Chưa mở gói'}</dd></div>
        <div><dt>Hình thức</dt><dd>{[teacher.supportsOnline && 'Online', teacher.supportsOffline && 'Trực tiếp'].filter(Boolean).join(' · ') || 'Chưa cập nhật'}</dd></div>
        {location && <div><dt>Nơi ở</dt><dd><EnvironmentOutlined aria-hidden="true" /> {location}</dd></div>}
      </dl>
      <div className="tm-teacher-preview-subjects">
        {teacher.subjects.slice(0, 4).map(subject => <span className="tm-chip" key={subject.id}>{subject.name}</span>)}
      </div>
      <Link className="tm-preview-link" href={`/teachers/${teacher.id}`}>Xem hồ sơ và lịch rảnh <ArrowRightOutlined aria-hidden="true" /></Link>
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
  if (isLoading) return <div className="tm-search-results-layout"><div className="tm-search-result-list">{[1, 2, 3].map(index => <div className="tm-panel" key={index}><Skeleton active avatar /></div>)}</div></div>;
  if (!teachers.length) return <EmptyState title="Chưa tìm thấy gia sư phù hợp" description="Thử bỏ bớt bộ lọc hoặc chọn môn học khác." />;

  return (
    <div className="tm-search-results-layout">
      <div className="tm-search-result-list" aria-label="Danh sách gia sư">
        {teachers.map(teacher => {
          const selected = activeId === teacher.id;
          const location = [teacher.wardName, teacher.provinceName].filter(Boolean).join(', ');
          return (
            <article className={`tm-search-result${selected ? ' is-active' : ''}`} key={teacher.id}>
              <button type="button" id={`teacher-card-${teacher.id}`} className="tm-search-result-main" aria-expanded={selected} aria-controls={`teacher-preview-${teacher.id}`} aria-label={`Xem nhanh ${teacher.fullName}`} onMouseEnter={() => setActiveId(teacher.id)} onFocus={() => setActiveId(teacher.id)} onClick={() => setActiveId(teacher.id)}>
                <span className="tm-search-result-avatar"><Avatar src={teacher.avatarUrl ?? undefined} alt="" size="lg" isVerified={teacher.verifiedBadge}>{teacher.fullName.slice(0, 1)}</Avatar></span>
                <span className="tm-search-result-content">
                  <span className="tm-search-result-heading"><strong>{teacher.fullName}</strong>{teacher.verifiedBadge && <span className="tm-verified-label">Đã xác minh</span>}</span>
                  <span className="tm-search-result-subjects">{teacher.subjects.slice(0, 3).map(subject => <span className="tm-chip" key={subject.id}>{subject.name}</span>)}</span>
                  <span className="tm-search-result-bio">{teacher.bioExcerpt || `${teacher.yearsOfExperience} năm kinh nghiệm giảng dạy.`}</span>
                  <span className="tm-search-result-meta">
                    {teacher.reviewCount > 0 ? <span><StarFilled aria-hidden="true" /> {teacher.averageRating.toFixed(1)} · {teacher.reviewCount} đánh giá</span> : <span>Chưa có đánh giá</span>}
                    <span>{teacher.yearsOfExperience} năm kinh nghiệm</span>
                    {location && <span><EnvironmentOutlined aria-hidden="true" /> {location}</span>}
                  </span>
                </span>
                <span className="tm-search-result-price">{teacher.startingPriceVnd > 0 ? <>Từ<strong>{money(teacher.startingPriceVnd)}đ</strong><small>trọn gói</small></> : <small>Chưa mở gói học</small>}</span>
                <span className="tm-search-result-open" aria-hidden="true"><ArrowRightOutlined /></span>
              </button>
              {selected && <QuickPreview teacher={teacher} mobile onClose={() => { setActiveId(undefined); document.getElementById(`teacher-card-${teacher.id}`)?.focus(); }} />}
            </article>
          );
        })}
      </div>
      <aside className="tm-search-preview-aside" aria-live="polite">
        {activeTeacher ? <QuickPreview teacher={activeTeacher} /> : <div className="tm-search-preview-prompt"><VideoCameraOutlined aria-hidden="true" /><strong>Xem nhanh hồ sơ gia sư</strong><span>Di chuột hoặc dùng phím Tab để xem thông tin.</span></div>}
      </aside>
    </div>
  );
}
