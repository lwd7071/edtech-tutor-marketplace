import React from 'react';
import { render, screen } from '@testing-library/react';
import TeachersPage from './page';
import { getPublicTeachers, getPublicSubjects } from '@/shared/api/public';

// Mock dependencies
jest.mock('@/shared/api/public', () => ({
  getPublicTeachers: jest.fn(),
  getPublicSubjects: jest.fn(),
}));

jest.mock('@/features/marketplace/components/TeacherGrid', () => {
  return function MockTeacherGrid({ teachers, isLoading }: any) {
    if (isLoading) return <div data-testid="teacher-grid-loading" />;
    return (
      <div data-testid="teacher-grid">
        {teachers?.map((t: any) => <span key={t.id}>{t.user?.fullName}</span>)}
      </div>
    );
  };
});

jest.mock('@/features/marketplace/components/TeacherFilterSidebar', () => ({
  TeacherFilterSidebar: () => <div data-testid="teacher-filter-sidebar" />
}));

jest.mock('@/features/marketplace/components/TeacherSortBar', () => ({
  TeacherSortBar: () => <div data-testid="teacher-sort-bar" />
}));

// Mock Next.js router hooks
jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: jest.fn(), replace: jest.fn() }),
  usePathname: () => '/teachers',
  useSearchParams: () => new URLSearchParams(),
}));

describe('TeachersPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders filters, sort bar, and fetches data', async () => {
    const mockTeacherData = {
      data: [{ id: '1', user: { fullName: 'Nguyen Van A' } }],
      meta: { page: 0, totalElements: 1, size: 20 }
    };
    (getPublicTeachers as jest.Mock).mockResolvedValue(mockTeacherData);
    
    const mockSubjectData = {
      data: [{ id: 'sub1', name: 'Toán' }],
      meta: { page: 0, totalElements: 1, size: 100 }
    };
    (getPublicSubjects as jest.Mock).mockResolvedValue(mockSubjectData);

    // Call Server Component function
    const searchParams = { keyword: 'Math' };
    const PageComponent = await TeachersPage({ searchParams });
    render(PageComponent);

    expect(screen.getByTestId('teacher-filter-sidebar')).toBeInTheDocument();
    expect(screen.getByTestId('teacher-sort-bar')).toBeInTheDocument();
    expect(screen.getByTestId('teacher-grid')).toBeInTheDocument();
    expect(screen.getByText('Nguyen Van A')).toBeInTheDocument();
    
    // Checks API calls
    expect(getPublicTeachers).toHaveBeenCalledWith(expect.objectContaining({ keyword: 'Math' }));
    expect(getPublicSubjects).toHaveBeenCalled();
  });
});
