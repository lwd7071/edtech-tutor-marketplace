'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Alert, Button, Input } from 'antd';
import { BookOutlined, SearchOutlined } from '@ant-design/icons';
import Link from 'next/link';
import type { SubjectSummary } from '@/shared/api/public';

const levels = [
  { value: '', label: 'Tất cả' },
  { value: 'ELEMENTARY', label: 'Tiểu học' },
  { value: 'MIDDLE_SCHOOL', label: 'THCS' },
  { value: 'HIGH_SCHOOL', label: 'THPT' },
  { value: 'UNIVERSITY', label: 'Đại học' },
  { value: 'OTHER', label: 'Khác' },
];

type Props = {
  subjects: SubjectSummary[];
  selectedSubject?: SubjectSummary;
  selectedUnavailable?: boolean;
  keyword: string;
  educationLevel: string;
  currentPage: number;
  totalPages: number;
  hasError: boolean;
};

export function SubjectPickerClient({
  subjects,
  selectedSubject,
  selectedUnavailable = false,
  keyword,
  educationLevel,
  currentPage,
  totalPages,
  hasError,
}: Props) {
  const router = useRouter();
  const [searchText, setSearchText] = useState(keyword);
  const [selected, setSelected] = useState(selectedSubject);
  const [filtersOpen, setFiltersOpen] = useState(false);

  useEffect(() => {
    setSearchText(keyword);
    setSelected(selectedSubject);
  }, [keyword, selectedSubject]);

  const navigate = (nextKeyword: string, nextLevel: string, page = 1, subjectId?: string) => {
    const query = new URLSearchParams();
    if (nextKeyword.trim()) query.set('keyword', nextKeyword.trim());
    if (nextLevel) query.set('educationLevel', nextLevel);
    if (page > 1) query.set('page', String(page));
    if (subjectId) query.set('subjectId', subjectId);
    router.push(`/subjects${query.size ? `?${query}` : ''}`);
  };

  const selectSubject = (subject: SubjectSummary) => {
    setSelected(subject);
    navigate(keyword, educationLevel, currentPage, subject.id);
  };

  const labels = Object.fromEntries(levels.map(level => [level.value, level.label]));

  return (
    <div className="tm-subject-picker">
      <section className="tm-subject-picker-intro" aria-labelledby="subject-picker-title">
        <div>
          <p className="tm-eyebrow">Bước 1 · Chọn môn học</p>
          <h1 id="subject-picker-title">Bạn muốn học môn nào?</h1>
          <p>Chọn môn và cấp học để tìm gia sư phù hợp với mục tiêu của bạn.</p>
        </div>
        {selected && <div className="tm-subject-picker-selected" aria-live="polite">
          <span>Môn đã chọn</span>
          <strong>{selected.name}</strong>
          {selected?.educationLevel && <small>{labels[selected.educationLevel] || selected.educationLevel}</small>}
        </div>}
      </section>

      {selectedUnavailable && (
        <Alert type="warning" showIcon message="Môn học này hiện không còn trong danh mục." description="Chọn một môn khác để tiếp tục tìm gia sư." />
      )}

      <button type="button" className="tm-subject-filter-toggle" aria-expanded={filtersOpen} aria-controls="subject-filter-controls" onClick={() => setFiltersOpen(value => !value)}>{filtersOpen ? 'Ẩn tìm kiếm và bộ lọc' : 'Tìm kiếm hoặc lọc môn học'} <span aria-hidden="true">{filtersOpen ? '−' : '+'}</span></button>
      <div id="subject-filter-controls" className={`tm-subject-filter-controls${filtersOpen ? ' is-open' : ''}`}>
      <form className="tm-subject-search" onSubmit={event => {
        event.preventDefault();
        setSelected(undefined);
        navigate(searchText, educationLevel);
      }}>
        <Input
          aria-label="Tìm môn học"
          value={searchText}
          onChange={event => setSearchText(event.target.value)}
          placeholder="Tìm theo tên môn học"
          prefix={<SearchOutlined aria-hidden="true" />}
          allowClear
          size="large"
        />
        <Button type="primary" htmlType="submit" size="large">Tìm môn</Button>
      </form>

      <div className="tm-subject-level-tabs" role="group" aria-label="Lọc theo cấp học">
        {levels.map(level => (
          <button
            type="button"
            key={level.value || 'all'}
            aria-pressed={educationLevel === level.value}
            className={educationLevel === level.value ? 'is-selected' : ''}
            onClick={() => {
              setSelected(undefined);
              navigate(keyword, level.value);
            }}
          >{level.label}</button>
        ))}
      </div>
      </div>

      {hasError ? (
        <div className="tm-subject-recovery"><Alert type="error" showIcon message="Chưa tải được danh sách môn học" description="Bạn có thể thử lại hoặc xem tất cả gia sư trong khi danh sách môn chưa sẵn sàng." action={<Button onClick={() => router.refresh()}>Tải lại</Button>} /><Link href="/teachers">Xem tất cả gia sư →</Link></div>
      ) : subjects.length ? (
        <div className="tm-subject-picker-list" aria-label="Danh sách môn học">
          {subjects.map(subject => (
            <button
              type="button"
              key={subject.id}
              className={`tm-subject-option${selected?.id === subject.id ? ' is-selected' : ''}`}
              aria-pressed={selected?.id === subject.id}
              onClick={() => selectSubject(subject)}
            >
              <span className="tm-subject-option-icon"><BookOutlined aria-hidden="true" /></span>
              <span className="tm-subject-option-copy">
                <strong>{subject.name}</strong>
                <small>{subject.educationLevel ? labels[subject.educationLevel] || subject.educationLevel : 'Môn học'}</small>
              </span>
              <span aria-hidden="true" className="tm-subject-option-arrow">→</span>
            </button>
          ))}
        </div>
      ) : (
        <div className="tm-empty-panel">
          <h2>Chưa tìm thấy môn học phù hợp</h2>
          <p>Thử một từ khóa khác hoặc xem tất cả cấp học.</p>
          <Button onClick={() => { setSearchText(''); setSelected(undefined); navigate('', ''); }}>Xem tất cả môn học</Button>
        </div>
      )}

      {totalPages > 1 && (
        <nav className="tm-pagination" aria-label="Phân trang môn học">
          {currentPage > 1 && <button className="tm-pagination-link" onClick={() => navigate(keyword, educationLevel, currentPage - 1, selected?.id)}>← Trang trước</button>}
          <span>Trang {currentPage} / {totalPages}</span>
          {currentPage < totalPages && <button className="tm-pagination-link" onClick={() => navigate(keyword, educationLevel, currentPage + 1, selected?.id)}>Trang sau →</button>}
        </nav>
      )}

      <div className="tm-subject-picker-footer">
        <Link href="/teachers">Xem tất cả gia sư</Link>
        <Button type="primary" size="large" disabled={!selected} onClick={() => selected && router.push(`/teachers?subjectId=${encodeURIComponent(selected.id)}`)}>
          Tìm gia sư{selected ? ` môn ${selected.name}` : ''}
        </Button>
      </div>
    </div>
  );
}
