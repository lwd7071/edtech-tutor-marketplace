'use client';

import React from 'react';
import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { TeacherFilterSidebar } from '@/features/marketplace/components/TeacherFilterSidebar';
import { TeacherSortBar } from '@/features/marketplace/components/TeacherSortBar';
import TeacherGrid from '@/features/marketplace/components/TeacherGrid';
import { TeacherSearchParams, SubjectSummary } from '@/shared/api/public';

interface TeacherSearchClientProps {
  initialFilters: TeacherSearchParams;
  initialTeachers: any; // ApiResponse<TeacherCard[]>
  subjects: SubjectSummary[];
}

export default function TeacherSearchClient({
  initialFilters,
  initialTeachers,
  subjects
}: TeacherSearchClientProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const handleFilterChange = (newFilters: Partial<TeacherSearchParams>) => {
    const params = new URLSearchParams(searchParams.toString());
    
    // Update params
    Object.entries(newFilters).forEach(([key, value]) => {
      if (value === undefined || value === '') {
        params.delete(key);
      } else {
        params.set(key, String(value));
      }
    });

    // Reset to page 1 on filter change
    if (!('page' in newFilters)) {
      params.delete('page');
    }

    router.push(`${pathname}?${params.toString()}`);
  };

  const handleClearFilters = () => {
    router.push(pathname);
  };

  const handlePageChange = (page: number) => {
    handleFilterChange({ page });
  };

  return (
    <div style={{ display: 'flex', gap: 'var(--space-8)', flexWrap: 'wrap' }}>
      {/* Sidebar */}
      <div style={{ flexShrink: 0, width: '100%', maxWidth: '280px' }}>
        <TeacherFilterSidebar 
          filters={initialFilters} 
          subjects={subjects}
          onChange={handleFilterChange}
          onClear={handleClearFilters}
        />
      </div>

      {/* Main Content */}
      <div style={{ flex: 1, minWidth: '300px' }}>
        <TeacherSortBar 
          totalElements={initialTeachers.meta?.totalElements || 0}
          value={initialFilters.sort}
          onChange={(sort) => handleFilterChange({ sort })}
        />
        
        <TeacherGrid 
          teachers={initialTeachers.data || []}
          isLoading={false}
          isError={false}
        />

        {initialTeachers.meta?.totalPages > 1 && (
          <div style={{ marginTop: 'var(--space-8)', display: 'flex', justifyContent: 'center' }} data-testid="pagination">
            <button 
              disabled={initialTeachers.meta.page === 0}
              onClick={() => handlePageChange(initialTeachers.meta.page)}
              style={{ marginRight: 'var(--space-2)', padding: 'var(--space-2) var(--space-4)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)', background: 'white', cursor: initialTeachers.meta.page === 0 ? 'not-allowed' : 'pointer', opacity: initialTeachers.meta.page === 0 ? 0.5 : 1 }}
            >
              Trang trước
            </button>
            <span style={{ display: 'flex', alignItems: 'center' }}>Trang {initialTeachers.meta.page + 1} / {initialTeachers.meta.totalPages}</span>
            <button 
              disabled={!initialTeachers.meta.hasNext}
              onClick={() => handlePageChange(initialTeachers.meta.page + 2)}
              style={{ marginLeft: 'var(--space-2)', padding: 'var(--space-2) var(--space-4)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)', background: 'white', cursor: !initialTeachers.meta.hasNext ? 'not-allowed' : 'pointer', opacity: !initialTeachers.meta.hasNext ? 0.5 : 1 }}
            >
              Trang sau
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
