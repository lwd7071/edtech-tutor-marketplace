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
    <div className="flex flex-col lg:flex-row gap-8">
      {/* Sidebar */}
      <div className="w-full lg:w-[280px] shrink-0">
        <TeacherFilterSidebar 
          filters={initialFilters} 
          subjects={subjects}
          onChange={handleFilterChange}
          onClear={handleClearFilters}
        />
      </div>

      {/* Main Content */}
      <div className="flex-1 min-w-0">
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

        {/* Note: Pagination component should be added here, currently just a placeholder if not present */}
        {initialTeachers.meta?.totalPages > 1 && (
          <div className="mt-8 flex justify-center" data-testid="pagination">
            <button 
              disabled={initialTeachers.meta.page === 0}
              onClick={() => handlePageChange(initialTeachers.meta.page)}
              className="mr-2 p-2"
            >
              Trang trước
            </button>
            <span>Trang {initialTeachers.meta.page + 1} / {initialTeachers.meta.totalPages}</span>
            <button 
              disabled={!initialTeachers.meta.hasNext}
              onClick={() => handlePageChange(initialTeachers.meta.page + 2)}
              className="ml-2 p-2"
            >
              Trang sau
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
