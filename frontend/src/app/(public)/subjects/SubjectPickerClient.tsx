'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Alert, Button, Input } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
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
  const [selected, setSelected] = useState<SubjectSummary[]>(selectedSubject ? [selectedSubject] : []);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [isNavigating, setIsNavigating] = useState(false);

  useEffect(() => {
    setSearchText(keyword);
    setSelected(selectedSubject ? [selectedSubject] : []);
    const readyTimer = window.setTimeout(() => setIsNavigating(false), 240);
    return () => window.clearTimeout(readyTimer);
  }, [keyword, selectedSubject, educationLevel, currentPage]);

  const navigate = (nextKeyword: string, nextLevel: string, page = 1, subjectIds: string[] = []) => {
    const query = new URLSearchParams();
    if (nextKeyword.trim()) query.set('keyword', nextKeyword.trim());
    if (nextLevel) query.set('educationLevel', nextLevel);
    if (page > 1) query.set('page', String(page));
    subjectIds.forEach(subjectId => query.append('subjectId', subjectId));
    setIsNavigating(true);
    router.push(`/subjects${query.size ? `?${query}` : ''}`);
  };

  const toggleSubject = (subject: SubjectSummary) => {
    const next = selected.some(item => item.id === subject.id)
      ? selected.filter(item => item.id !== subject.id)
      : [...selected, subject];
    setSelected(next);
    navigate(keyword, educationLevel, currentPage, next.map(item => item.id));
  };

  const labels = Object.fromEntries(levels.map(level => [level.value, level.label]));

  return (
    <div className="tm-subject-picker">
      <header className="tm-subject-picker-intro">
        <p className="tm-eyebrow">Bước 1 · Chọn môn học</p>
        <h1 id="subject-picker-title">Bạn muốn học môn nào?</h1>
        <p>Chọn môn và cấp học để tìm gia sư phù hợp với mục tiêu của bạn.</p>
      </header>

      {selectedUnavailable && (
        <Alert
          type="warning"
          showIcon
          title="Môn học này hiện không còn trong danh mục."
          description="Chọn một môn khác để tiếp tục tìm gia sư."
        />
      )}

      <section className="tm-subject-controls" aria-label="Tìm kiếm và lọc môn học">
        <button
          type="button"
          className="tm-subject-filter-toggle"
          aria-expanded={filtersOpen}
          aria-controls="subject-filter-controls"
          onClick={() => setFiltersOpen(value => !value)}
        >
          {filtersOpen ? 'Ẩn tìm kiếm và bộ lọc' : 'Tìm kiếm hoặc lọc môn học'}
          <span aria-hidden="true">{filtersOpen ? '−' : '+'}</span>
        </button>

        <div id="subject-filter-controls" className={`tm-subject-filter-controls${filtersOpen ? ' is-open' : ''}`}>
          <div className="tm-subject-filter-row">
            <Input.Search
              className="tm-subject-search"
              aria-label="Tìm môn học"
              value={searchText}
              onChange={event => setSearchText(event.target.value)}
              placeholder="Tìm theo tên môn học"
              prefix={<SearchOutlined aria-hidden="true" />}
              allowClear
              enterButton="Tìm môn"
              onSearch={value => {
                setSelected([]);
                navigate(value, educationLevel);
              }}
            />
            <label className="tm-subject-level-select">
              <span>Cấp học</span>
              <select
                aria-label="Lọc theo cấp học"
                value={educationLevel}
                onChange={event => {
                  setSelected([]);
                  navigate(keyword, event.target.value);
                }}
              >
                {levels.map(level => <option key={level.value || 'all'} value={level.value}>{level.label}</option>)}
              </select>
            </label>
          </div>
        </div>
      </section>

      <section className="tm-subject-results" aria-labelledby="subject-results-title">
        <header className="tm-subject-results-heading">
          <h2 id="subject-results-title">Danh mục môn học</h2>
          {subjects.length > 0 && !hasError && <span>{subjects.length} môn trên trang này</span>}
        </header>

        {isNavigating ? (
          <div className="tm-subject-results-loading" role="status" aria-live="polite">
            <span>Đang tải danh sách môn học</span>
            <div aria-hidden="true">
              {Array.from({ length: 3 }, (_, index) => <span className="tm-subject-skeleton tm-subject-skeleton-row" key={index} />)}
            </div>
          </div>
        ) : hasError ? (
          <div className="tm-subject-recovery">
            <Alert
              type="error"
              showIcon
              title="Chưa tải được danh sách môn học"
              description="Bạn có thể thử lại hoặc xem tất cả gia sư trong khi danh sách môn chưa sẵn sàng."
              action={<Button onClick={() => router.refresh()}>Tải lại</Button>}
            />
          </div>
        ) : subjects.length ? (
          <fieldset className="tm-subject-picker-list" aria-label="Danh sách môn học">
            <legend>Chọn một hoặc nhiều môn học</legend>
            {subjects.map(subject => (
              <label className={`tm-subject-option${selected.some(item => item.id === subject.id) ? ' is-selected' : ''}`} key={subject.id}>
                <input
                  type="checkbox"
                  value={subject.id}
                  checked={selected.some(item => item.id === subject.id)}
                  onChange={() => toggleSubject(subject)}
                />
                <span className="tm-subject-option-copy">
                  <strong>{subject.name}</strong>
                  <small>{subject.educationLevel ? labels[subject.educationLevel] || subject.educationLevel : 'Môn học'}</small>
                </span>
                {selected.some(item => item.id === subject.id) && <span className="tm-subject-option-status">Đã chọn</span>}
              </label>
            ))}
          </fieldset>
        ) : (
          <section className="tm-empty-panel" aria-labelledby="subject-empty-title">
            <h2 id="subject-empty-title">Chưa tìm thấy môn học phù hợp</h2>
            <p>Thử một từ khóa khác hoặc xem tất cả cấp học.</p>
            <Button onClick={() => { setSearchText(''); setSelected([]); navigate('', ''); }}>Xem tất cả môn học</Button>
          </section>
        )}
      </section>

      <section className="tm-subject-picker-footer" aria-label="Tiếp tục tìm gia sư">
        <div className="tm-subject-selection-summary" aria-live="polite">
          {selected.length ? (
            <>
              <span>Môn đã chọn ({selected.length})</span>
              <strong>{selected.map(item => item.name).join(', ')}</strong>
            </>
          ) : (
            <><strong>Chưa chọn môn học</strong><span>Chọn môn để lọc gia sư phù hợp, hoặc xem tất cả gia sư.</span></>
          )}
        </div>
        <div className="tm-subject-picker-actions">
          <Link href="/teachers">Xem tất cả gia sư</Link>
          <Button type="primary" size="large" onClick={() => {
            const query = new URLSearchParams();
            selected.forEach(item => query.append('subjectId', item.id));
            router.push(`/teachers${query.size ? `?${query}` : ''}`);
          }}>
            Tìm gia sư
          </Button>
        </div>
      </section>

      {totalPages > 1 && (
        <nav className="tm-pagination tm-subject-pagination" aria-label="Phân trang môn học">
          {currentPage > 1 && <button className="tm-pagination-link" onClick={() => navigate(keyword, educationLevel, currentPage - 1, selected.map(item => item.id))}>Trang trước</button>}
          <span>Trang {currentPage} / {totalPages}</span>
          {currentPage < totalPages && <button className="tm-pagination-link" onClick={() => navigate(keyword, educationLevel, currentPage + 1, selected.map(item => item.id))}>Trang sau</button>}
        </nav>
      )}
    </div>
  );
}
