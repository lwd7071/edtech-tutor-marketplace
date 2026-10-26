'use client';

import { useState } from 'react';
import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { Button, Drawer, Input, Pagination } from 'antd';
import { FilterOutlined } from '@ant-design/icons';
import { TeacherFilterSidebar } from '@/features/marketplace/components/TeacherFilterSidebar';
import { TeacherSortBar } from '@/features/marketplace/components/TeacherSortBar';
import { TeacherSearchResults } from '@/features/marketplace/components/TeacherSearchResults';
import type { TeacherSearchParams, SubjectSummary, TeacherCard, PageMeta } from '@/shared/api/public';

export default function TeacherSearchClient({ initialFilters, initialTeachers, subjects, isError = false }: {
  initialFilters: TeacherSearchParams;
  initialTeachers: { data: TeacherCard[]; meta: Partial<PageMeta> };
  subjects: SubjectSummary[];
  isError?: boolean;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const search = useSearchParams();
  const [open, setOpen] = useState(false);
  const activeCount = ['subjectId', 'deliveryMode', 'minPrice', 'maxPrice', 'minRating', 'provinceCode', 'wardCode', 'dayOfWeek']
    .filter(key => initialFilters[key as keyof TeacherSearchParams] !== undefined
      && initialFilters[key as keyof TeacherSearchParams] !== ''
      && initialFilters[key as keyof TeacherSearchParams] !== null).length;

  const change = (values: Partial<TeacherSearchParams>) => {
    const query = new URLSearchParams(search.toString());
    Object.entries(values).forEach(([key, value]) => {
      if (value === undefined || value === null || value === '') query.delete(key);
      else query.set(key, String(value));
    });
    if (!('page' in values)) query.delete('page');
    router.push(pathname + '?' + query.toString());
  };
  const filters = <TeacherFilterSidebar
    filters={initialFilters}
    subjects={subjects}
    onChange={change}
    onClear={() => router.push(pathname)}
  />;
  const appliedFilters = [
    initialFilters.subjectId && { label: subjects.find(subject => subject.id === initialFilters.subjectId)?.name || 'Môn đã chọn', clear: () => change({ subjectId: undefined }) },
    initialFilters.deliveryMode && { label: initialFilters.deliveryMode === 'ONLINE' ? 'Online' : 'Trực tiếp', clear: () => change({ deliveryMode: undefined }) },
    (initialFilters.minPrice !== undefined || initialFilters.maxPrice !== undefined) && { label: `Giá gói: ${initialFilters.minPrice?.toLocaleString('vi-VN') || '0'} – ${initialFilters.maxPrice?.toLocaleString('vi-VN') || 'không giới hạn'}đ`, clear: () => change({ minPrice: undefined, maxPrice: undefined }) },
    initialFilters.minRating !== undefined && { label: `Từ ${initialFilters.minRating} sao`, clear: () => change({ minRating: undefined }) },
    initialFilters.dayOfWeek && { label: 'Lịch rảnh đã chọn', clear: () => change({ dayOfWeek: undefined, startTime: undefined, endTime: undefined }) },
  ].filter((filter): filter is { label: string; clear: () => void } => Boolean(filter));

  return (
    <div className="tm-teacher-search">
      <section className="tm-teacher-search-controls" aria-label="Tìm và lọc gia sư">
        <div className="tm-teacher-search-topline">
          <label className="tm-teacher-search-label" htmlFor="teacher-keyword">Tìm trong hồ sơ gia sư</label>
          <Button className="tm-mobile-only tm-filter-trigger" icon={<FilterOutlined aria-hidden="true" />} onClick={() => setOpen(true)}>
            Bộ lọc{activeCount > 0 ? ` (${activeCount})` : ''}
          </Button>
        </div>
        <Input.Search
          id="teacher-keyword"
          aria-label="Tìm gia sư theo từ khóa"
          placeholder="Tên gia sư hoặc từ khóa"
          defaultValue={initialFilters.keyword}
          onSearch={keyword => change({ keyword })}
          allowClear
          size="large"
        />
        {appliedFilters.length > 0 && <ul className="tm-teacher-active-filters" aria-label="Bộ lọc đang áp dụng">
          {appliedFilters.map(filter => <li key={filter.label}><button type="button" onClick={filter.clear} aria-label={`Bỏ lọc: ${filter.label}`}><span>{filter.label}</span><span aria-hidden="true">×</span></button></li>)}
        </ul>}
      </section>

      <div className="tm-teacher-search-workspace">
        <aside className="tm-filter-desktop" aria-label="Bộ lọc gia sư">{filters}</aside>
        <main className="tm-teacher-search-main">
          <TeacherSortBar totalElements={initialTeachers.meta.totalElements || 0} value={initialFilters.sort} onChange={sort => change({ sort })} />
          <TeacherSearchResults teachers={initialTeachers.data} isError={isError} onRetry={() => router.refresh()} />
          {(initialTeachers.meta.totalPages || 0) > 1 && (
            <nav className="tm-pagination" aria-label="Trang kết quả gia sư">
              <Pagination current={(initialTeachers.meta.page || 0) + 1} total={initialTeachers.meta.totalElements} pageSize={initialTeachers.meta.size || 12} showSizeChanger={false} onChange={page => change({ page })} />
            </nav>
          )}
        </main>
      </div>

      <Drawer title="Lọc gia sư" open={open} onClose={() => setOpen(false)} footer={<Button type="primary" block onClick={() => setOpen(false)}>Xem kết quả</Button>}>
        {filters}
      </Drawer>
    </div>
  );
}
