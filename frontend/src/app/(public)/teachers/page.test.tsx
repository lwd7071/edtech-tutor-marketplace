import React from 'react';
import { render, screen } from '@testing-library/react';
import TeachersPage from './page';
import { getPublicTeachersServer, getPublicSubjectsServer } from '@/shared/api/public.server';

// Mock dependencies
jest.mock('@/shared/api/public.server', () => ({
  getPublicTeachersServer: jest.fn(),
  getPublicSubjectsServer: jest.fn(),
}));

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
      data: [{ id: '1', fullName: 'Nguyen Van A', yearsOfExperience: 3, verifiedBadge: true, supportsOnline: true, supportsOffline: false, subjects: [], startingPriceVnd: 250000, averageRating: 4.8, bayesianRating: 4.7, reviewCount: 12, globalRank: 4 }],
      meta: { page: 0, totalElements: 1, size: 20 }
    };
    (getPublicTeachersServer as jest.Mock).mockResolvedValue(mockTeacherData);
    
    const mockSubjectData = {
      data: [{ id: 'sub1', name: 'Toán' }],
      meta: { page: 0, totalElements: 1, size: 11 }
    };
    (getPublicSubjectsServer as jest.Mock).mockResolvedValue(mockSubjectData);

    // Call Server Component function
    const searchParams = { keyword: 'Math' };
    const PageComponent = await TeachersPage({ searchParams: Promise.resolve(searchParams) });
    render(PageComponent);

    expect(screen.getByTestId('teacher-filter-sidebar')).toBeInTheDocument();
    expect(screen.getByTestId('teacher-sort-bar')).toBeInTheDocument();
    expect(screen.getByText('Nguyen Van A')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Xem nhanh Nguyen Van A' })).toBeInTheDocument();
    
    // Checks API calls
    expect(getPublicTeachersServer).toHaveBeenCalledWith(expect.objectContaining({ keyword: 'Math' }));
    expect(getPublicSubjectsServer).toHaveBeenCalledWith({ size: 11 });
  });
});


