import React from 'react';
import { render, screen } from '@testing-library/react';
import TeacherDetailPage from './page';
import { getTeacherDetail, getTeacherPackages, getTeacherAvailability, getTeacherReviews } from '@/shared/api/public';

jest.mock('@/shared/api/public', () => ({
  getTeacherDetail: jest.fn(),
  getTeacherPackages: jest.fn(),
  getTeacherAvailability: jest.fn(),
  getTeacherReviews: jest.fn(),
}));

jest.mock('@/features/marketplace/components/TeacherProfileHeader', () => ({
  TeacherProfileHeader: () => <div data-testid="teacher-profile-header" />
}));

jest.mock('@/features/marketplace/components/TeacherPackagesTab', () => ({
  TeacherPackagesTab: () => <div data-testid="teacher-packages-tab" />
}));

jest.mock('@/features/marketplace/components/TeacherReviewsTab', () => ({
  TeacherReviewsTab: () => <div data-testid="teacher-reviews-tab" />
}));

jest.mock('@/shared/components/data-display/WeeklyScheduleGrid', () => ({
  WeeklyScheduleGrid: () => <div data-testid="weekly-schedule-grid" />
}));

// Mock window.matchMedia for Ant Design Tabs
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(), // Deprecated
    removeListener: jest.fn(), // Deprecated
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: jest.fn(), replace: jest.fn() }),
  usePathname: () => '/teachers/1',
  useSearchParams: () => new URLSearchParams(),
}));

describe('TeacherDetailPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders correctly and calls APIs', async () => {
    (getTeacherDetail as jest.Mock).mockResolvedValue({ id: '1', fullName: 'John Doe', bio: 'Bio' });
    (getTeacherPackages as jest.Mock).mockResolvedValue({ data: [], meta: {} });
    (getTeacherAvailability as jest.Mock).mockResolvedValue([]);
    (getTeacherReviews as jest.Mock).mockResolvedValue({ data: [], meta: {} });

    // Server Component call
    const PageComponent = await TeacherDetailPage({ params: Promise.resolve({ id: '1' }), searchParams: Promise.resolve({}) });
    render(PageComponent);

    expect(screen.getByTestId('teacher-profile-header')).toBeInTheDocument();
    expect(screen.getByText('Giới thiệu')).toBeInTheDocument();
    expect(screen.getByText('Bio')).toBeInTheDocument();
    
    expect(getTeacherDetail).toHaveBeenCalledWith('1');
    expect(getTeacherPackages).toHaveBeenCalledWith('1', 0, 20);
    expect(getTeacherAvailability).toHaveBeenCalledWith('1');
    expect(getTeacherReviews).toHaveBeenCalledWith('1', 0, 10);
  });
});

