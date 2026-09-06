import React from 'react';
import { Pagination, Space } from 'antd';
import { getPublicSubjects } from '@/shared/api/public';
import SubjectGrid from '@/features/marketplace/components/SubjectGrid';
import SubjectSearchInput from './SubjectSearchInput';


export default async function SubjectsPage({
  searchParams,
}: {
  searchParams: { [key: string]: string | string[] | undefined };
}) {
  const keyword = typeof searchParams.keyword === 'string' ? searchParams.keyword : undefined;
  const pageStr = typeof searchParams.page === 'string' ? searchParams.page : '1';
  const page = parseInt(pageStr, 10);
  const size = 20; // Default size

  let subjects: any[] = [];
  let meta = { totalElements: 0, page: 0, size };
  let isError = false;

  try {
    const res = await getPublicSubjects({
      keyword,
      page: page > 0 ? page - 1 : 0, // Backend is 0-indexed
      size,
    });
    subjects = res.data;
    meta = res.meta;
  } catch (error) {
    isError = true;
  }

  return (
    <div style={{ maxWidth: 'var(--size-container-wide)', margin: '0 auto', padding: 'var(--space-12) var(--space-4)' }}>
      <div style={{ textAlign: 'center', marginBottom: 'var(--space-12)' }}>
        <h1 style={{ margin: '0 0 var(--space-4) 0', fontSize: 'var(--text-h1)', fontWeight: 600 }}>Danh mục môn học</h1>
        <p style={{ fontSize: '18px', color: 'var(--color-text-secondary)', margin: 0 }}>
          Khám phá các môn học đa dạng và tìm giáo viên phù hợp với bạn
        </p>
        
        <div style={{ maxWidth: '400px', margin: 'var(--space-6, 24px) auto 0' }}>
          <SubjectSearchInput defaultValue={keyword} />
        </div>
      </div>

      <SubjectGrid 
        subjects={subjects}
        isLoading={false} // Server component, we can use React Suspense with loading.tsx if needed, but for simplicity here we assume loaded
        isError={isError}
      />

      {!isError && meta.totalElements > 0 && (
        <div style={{ display: 'flex', justifyContent: 'center', marginTop: 'var(--space-8, 32px)' }}>
          {/* We use a client wrapper or a simple anchor based pagination for server components, or if Pagination is client side we need to pass router. */}
          {/* For simplicity, we can use a client component for pagination or just let Antd Pagination handle onChange by pushing to router */}
          <Pagination
            current={meta.page + 1}
            total={meta.totalElements}
            pageSize={meta.size}
            showSizeChanger={false}
            itemRender={(page, type, originalElement) => {
              if (type === 'page') {
                return <a href={`/subjects?page=${page}${keyword ? `&keyword=${keyword}` : ''}`}>{page}</a>;
              }
              if (type === 'prev') {
                return <a href={`/subjects?page=${meta.page}${keyword ? `&keyword=${keyword}` : ''}`}>Prev</a>;
              }
              if (type === 'next') {
                return <a href={`/subjects?page=${meta.page + 2}${keyword ? `&keyword=${keyword}` : ''}`}>Next</a>;
              }
              return originalElement;
            }}
          />
        </div>
      )}
    </div>
  );
}
