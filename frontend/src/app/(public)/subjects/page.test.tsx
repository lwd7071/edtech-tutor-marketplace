import React from 'react';
import { render, screen } from '@testing-library/react';
import SubjectsPage from './page';
import { getPublicSubjects } from '@/shared/api/public';

// Mock dependencies
jest.mock('@/shared/api/public', () => ({
  getPublicSubjects: jest.fn(),
}));

jest.mock('@/features/marketplace/components/SubjectGrid', () => {
  return function MockSubjectGrid({ subjects, isLoading, isError }: any) {
    if (isLoading) return <div data-testid="subject-grid-loading" />;
    if (isError) return <div data-testid="subject-grid-error" />;
    return (
      <div data-testid="subject-grid">
        {subjects?.map((s: any) => <span key={s.id}>{s.name}</span>)}
      </div>
    );
  };
});

jest.mock('@/shared/components/ui/DebouncedSearch', () => {
  return function MockDebouncedSearch() {
    return <input data-testid="debounced-search" />;
  };
});

// Mock Next.js Link and useRouter
jest.mock('next/link', () => {
  return ({ children }: any) => {
    return children;
  };
});
jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: jest.fn() }),
  usePathname: () => '/subjects',
  useSearchParams: () => new URLSearchParams(),
}));

// Next.js Page components can be async in App Router
// React testing library render doesn't support async components directly without a wrapper in older versions,
// but for simple testing we can test it by awaiting it if we call it as a function or just mock the data.
// Since it's a server component in App Router, we'll test it as an async function that returns JSX.

describe('SubjectsPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders search input and fetches subjects', async () => {
    const mockData = {
      data: [{ id: '1', name: 'Môn Toán' }],
      meta: { page: 0, totalElements: 1, size: 20 }
    };
    (getPublicSubjects as jest.Mock).mockResolvedValue(mockData);

    // Call the async Server Component
    const searchParams = { keyword: 'Toán', page: '1' };
    const PageComponent = await SubjectsPage({ searchParams });
    render(PageComponent);

    expect(screen.getByTestId('debounced-search')).toBeInTheDocument();
    expect(screen.getByTestId('subject-grid')).toBeInTheDocument();
    expect(screen.getByText('Môn Toán')).toBeInTheDocument();
    
    // Check if API was called with correct mapped params (page - 1 for 0-indexed API)
    expect(getPublicSubjects).toHaveBeenCalledWith({
      keyword: 'Toán',
      page: 0,
      size: 20
    });
  });

  it('handles error state', async () => {
    (getPublicSubjects as jest.Mock).mockRejectedValue(new Error('Network error'));

    const PageComponent = await SubjectsPage({ searchParams: {} });
    render(PageComponent);

    expect(screen.getByTestId('subject-grid-error')).toBeInTheDocument();
  });
});
